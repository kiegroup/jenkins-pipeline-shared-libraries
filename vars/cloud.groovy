/*
* Make a quay.io image public if not already
*/
void makeQuayImagePublic(String namespace, String repository, Map credentials = [ 'token': '', 'usernamePassword': '' ]) {
    if (!isQuayImagePublic(namespace, repository, credentials)
        && !setQuayImagePublic(namespace, repository, credentials)) {
        error "Cannot set image quay.io/${namespace}/${repository} as visible"
    }
}

/*
* Checks whether a quay image is public
*/
boolean isQuayImagePublic(String namespace, String repository, Map credentials = [ 'token': '', 'usernamePassword': '' ]) {
    def output = 'false'
    util.executeWithCredentialsMap(credentials) {
        output = sh(returnStdout: true, script: "curl -H 'Authorization: Bearer ${QUAY_TOKEN}' -X GET https://quay.io/api/v1/repository/${namespace}/${repository} | jq '.is_public'").trim()
    }
    return output == 'true'
}

/*
* Sets a Quay repository as public
*
* return false if any problem occurs
*/
boolean setQuayImagePublic(String namespace, String repository, Map credentials = [ 'token': '', 'usernamePassword': '' ]) {
    def output = 'false'
    util.executeWithCredentialsMap(credentials) {
        output = sh(returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer ${QUAY_TOKEN}' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/${namespace}/${repository}/changevisibility | jq '.success'").trim()
    }
    return output == 'true'
}

/*
* Cleanup all containers and images
*/
void cleanContainersAndImages(String containerEngine = 'podman') {
    println '[INFO] Cleaning up running containers and images. Any error here can be ignored'
    sh(script: "${containerEngine} ps -a -q | tr '\\n' ','", returnStdout: true).trim().split(',').findAll{ it != ''}.each {
        sh "${containerEngine} rm -f ${it} || date"
    }
    sh(script: "${containerEngine} images -q | tr '\\n' ','", returnStdout: true).trim().split(',').findAll{ it != ''}.each {
        sh "${containerEngine} rmi -f ${it} || date"
    }
}

/*
* Start local docker registry
*
* Accessible on `localhost:${port}`. Default port is 5000.
*/
void startLocalRegistry(int port = 5000) {
    cleanLocalRegistry()
    sh "docker run -d -p ${port}:5000 --restart=always --name registry-${port} --name registry registry:2"
    sh 'docker ps'
}

/*
* Clean local registry
*/
void cleanLocalRegistry(int port = 5000) {
    sh "docker rm -f registry-${port} || true"
    sh 'docker ps'
}

/*
* Install skopeo CLI via yum
*
* https://github.com/containers/skopeo
*/
void installSkopeo() {
    cleanSkopeo()
    sh '''
        sudo yum -y install --nobest skopeo
        skopeo --version
    '''
}

/*
* Remove skopeo from the node
*/
void cleanSkopeo() {
    sh 'sudo yum -y remove skopeo || true'
}

/*
* Squash a docker image
*/
void dockerSquashImage(String baseImage, String squashMessage) {
    String squashedPlatformImage = "${baseImage}"

    // Squash images
    def nbLayers = Integer.parseInt(sh(returnStdout: true, script: "docker history ${baseImage} | grep buildkit.dockerfile | wc -l").trim())
    nbLayers++ // Get the next layer not done by buildkit
    echo "Got ${nbLayers} layers to squash"
    // Use message option in docker-squash due to https://github.com/goldmann/docker-squash/issues/220
    runPythonCommand("""
        docker-squash -v -m '${squashMessage}' -f ${nbLayers} -t ${squashedPlatformImage} ${baseImage} 
        docker push ${squashedPlatformImage}
    """)

    return squashedPlatformImage
}

/*
* Print some debugging for a specific image
*/
void dockerDebugImage(String imageTag) {
    sh """
        docker images
        docker history ${imageTag}
        docker inspect ${imageTag}
    """
}

//////////////////////////////////////////////////////////////////////////////////////
// Multiplatform build

/*
* Build an image for multiple platforms and create a manifest to gather under a same name
*
* You should have run `prepareForDockerMultiplatformBuild` method before executing this method
*/
void dockerBuildMultiPlatformImages(String buildImageTag, List platforms, boolean squashImages = true, String squashMessage = "Squashed ${buildImageTag}") {
    // Build image locally in tgz file
    List buildPlatformImages = platforms.collect { platform ->
        String os_arch = platform.replaceAll('/', '-')
        String platformImage = "${buildImageTag}-${os_arch}"
        String finalPlatformImage = platformImage

        // Build
        dockerBuildPlatformImage(platformImage, platform, debug)
        if (debug) { dockerDebugImage(platformImage) }

        if (squashImages) {
            finalPlatformImage = dockerSquashImage(platformImage, squashMessage)
            if (debug) { dockerDebugImage(platformImage) }
        }

        return finalPlatformImage
    }

    dockerCreateManifest(buildImageTag, buildPlatformImages)
    if (debug) { dockerDebugImage(buildImageTag) }
}

/*
* Build an image for a specific platform
*
* You should have run `prepareForDockerMultiplatformBuild` method before executing this method
*/
void dockerBuildPlatformImage(String buildImageTag, String platform) {
    sh """
        docker buildx build --push --sbom=false --provenance=false --platform ${platform} -t ${buildImageTag} .
        docker buildx imagetools inspect ${buildImageTag}
        docker pull --platform ${platform} ${buildImageTag}
    """
}

/*
* Prepare the node for Docker multiplatform build
*/
void prepareForDockerMultiplatformBuild(boolean debug = false) {
    cleanDockerMultiplatformBuild()

    // For multiplatform build
    sh 'docker run --rm --privileged --name binfmt docker.io/tonistiigi/binfmt --install all'

    // Debug purpose
    if (debug) {
        sh 'docker context ls'
        sh 'docker buildx ls'
        sh 'docker ps'
    }

    writeFile(file: 'buildkitd.toml', text: '''
debug = true
[registry."docker.io"]
mirrors = ["mirror.gcr.io"]
[registry."localhost:5000"]
http = true
        ''')

    sh 'docker buildx create --name mybuilder --driver docker-container --driver-opt network=host --bootstrap --config ${WORKSPACE}/buildkitd.toml'
    sh 'docker buildx use mybuilder'

    if (debug) {
        sh 'docker buildx inspect'
        sh 'docker buildx ls'
        sh 'docker ps'
    }
}

/*
* Clean the node from Docker multiplatform configuration
*/
void cleanDockerMultiplatformBuild() {
    sh 'docker buildx rm mybuilder || true'
    sh 'docker rm -f binfmt || true'
    sh 'docker buildx ls'
    sh 'docker ps'
}

/*
* Create a multiplatform manifest based on the given images
*/
void dockerCreateManifest(String buildImageTag, List manifestImages) {
    sh """
        docker manifest rm ${buildImageTag} || true
        docker manifest create ${buildImageTag} --insecure ${manifestImages.join(' ')}
        docker manifest push ${buildImageTag}
    """
}