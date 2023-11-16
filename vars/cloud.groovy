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
* Login to given OpenShift API
*/
void loginOpenShift(String openShiftAPI, String openShiftCredsId) {
    withCredentials([usernamePassword(credentialsId: openShiftCredsId, usernameVariable: 'OC_USER', passwordVariable: 'OC_PWD')]) {
        sh "oc login --username=${OC_USER} --password=${OC_PWD} --server=${openShiftAPI} --insecure-skip-tls-verify"
    }
}

/*
* Login to current OpenShift registry
*
* It considers that you are already authenticated to OpenShift
*/
void loginOpenShiftRegistry(String containerEngine = 'docker', String containerEngineTlsOptions = '') {
    // username can be anything. See https://docs.openshift.com/container-platform/4.4/registry/accessing-the-registry.html#registry-accessing-directly_accessing-the-registry
    sh "set +x && ${containerEngine} login -u anything -p \$(oc whoami -t) ${containerEngineTlsOptions} ${getOpenShiftRegistryURL()}"
}

/*
* Retrieve the OpenShift registry URL
*
* It considers that you are already authenticated to OpenShift
*/
String getOpenShiftRegistryURL() {
    return sh(returnStdout: true, script: "oc get routes -n openshift-image-registry | tail -1 | awk '{print \$2}'")?.trim()
}

/*
* Login to a container registry
*/
void loginContainerRegistry(String registry, String credsId, String containerEngine = 'docker', String containerEngineTlsOptions = '') {
    withCredentials([usernamePassword(credentialsId: credsId, usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PWD')]) {
        sh "set +x && ${containerEngine} login -u ${REGISTRY_USER} -p ${REGISTRY_PWD} ${containerEngineTlsOptions} ${registry}"
    }
}

void pullImage(String imageTag, int retries = 3, String containerEngine = 'docker', String containerEngineTlsOptions = '') {
    retry(retries) {
        sh "${containerEngine} pull ${containerEngineTlsOptions} ${imageTag}"
    }
}

void pushImage(String imageTag, int retries = 3, String containerEngine = 'docker', String containerEngineTlsOptions = '') {
    retry(retries) {
        sh "${containerEngine} push ${containerEngineTlsOptions} ${imageTag}"
    }
}

void tagImage(String oldImageTag, String newImageTag, String containerEngine = 'docker') {
    sh "${containerEngine} tag ${oldImageTag} ${newImageTag}"
}

/*
* Cleanup all containers and images
*/
void cleanContainersAndImages(String containerEngine = 'podman') {
    println '[INFO] Cleaning up running containers and images. Any error here can be ignored'
    sh(script: "${containerEngine } ps -a -q | tr '\\n' ','", returnStdout: true).trim().split(',').findAll { it != '' }.each {
        sh "${containerEngine} rm -f ${it} || date"
}
    sh(script: "${containerEngine } images -q | tr '\\n' ','", returnStdout: true).trim().split(',').findAll { it != '' }.each {
        sh "${containerEngine} rmi -f ${it} || date"
    }
}

/*
* Start local docker registry
*
* Accessible on `localhost:${port}`. Default port is 5000.
*/
String startLocalRegistry(int port = 5000) {
    cleanLocalRegistry(port)
    sh "docker run -d -p ${port}:5000 --restart=always --name registry-${port} registry:2"
    sh 'docker ps'
    return "localhost:${port}"
}

/*
* Find an open local port.
*/
int findFreePort() {
    return Integer.valueOf(sh( script: """ echo \$(python -c 'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()') """, returnStdout: true ).trim())
}

/*
* Clean local registry
*/
void cleanLocalRegistry(int port = 5000) {
    sh "docker rm -f registry-${port} || true"
    sh 'docker ps'
}

/*
* Squash a docker image
*
* If `replaceCurrentImage` is disabled, the `-squashed` suffix is added to the returned image name
*/
String dockerSquashImage(String baseImage, String squashMessage = "${baseImage} squashed", boolean replaceCurrentImage = true) {
    String squashedPlatformImage = replaceCurrentImage ? "${baseImage}" : "${baseImage}-squashed"

    // Squash images
    def nbLayers = Integer.parseInt(sh(returnStdout: true, script: "docker history ${baseImage} | grep buildkit.dockerfile | wc -l").trim())
    nbLayers++ // Get the next layer not done by buildkit
    echo "Got ${nbLayers} layers to squash"
    // Use message option in docker-squash due to https://github.com/goldmann/docker-squash/issues/220
    util.runWithPythonVirtualEnv("docker-squash -v -m '${squashMessage}' -f ${nbLayers} -t ${squashedPlatformImage} ${baseImage}", 'cekit')
    sh "docker push ${squashedPlatformImage}"

    return squashedPlatformImage
}

/*
* Print some debugging for a specific image
*/
void dockerDebugImage(String imageTag) {
    sh 'docker images'
    sh "docker history ${imageTag}"
    sh "docker inspect ${imageTag}"
}

//////////////////////////////////////////////////////////////////////////////////////
// Multiplatform build

/*
* Build an image for multiple platforms and create a manifest to gather under a same name
*
* You should have run `prepareForDockerMultiplatformBuild` method before executing this method
*/
void dockerBuildMultiPlatformImages(String buildImageTag, List platforms, boolean squashImages = true, String squashMessage = "Squashed ${buildImageTag}", boolean debug = false, boolean outputToFile = false) {
    // Build image locally in tgz file
    List buildPlatformImages = platforms.collect { platform ->
        String os_arch = platform.replaceAll('/', '-')
        String platformImage = "${buildImageTag}-${os_arch}"
        String finalPlatformImage = platformImage

        // Build
        dockerBuildPlatformImage(platformImage, platform, outputToFile)
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
void dockerBuildPlatformImage(String buildImageTag, String platform, boolean outputToFile = false) {
    def logFileName = (buildImageTag + '-' + platform + '-build.log')
        .replaceAll('/','_')
        .replaceAll(':','_')
    sh "docker buildx build --push --sbom=false --provenance=false --platform ${platform} -t ${buildImageTag} .${outputToFile ? ' &> ' + logFileName : ''}"
    sh "docker buildx imagetools inspect ${buildImageTag}"
    sh "docker pull --platform ${platform} ${buildImageTag}"
}

/*
* Create a multiplatform manifest based on the given images
*/
void dockerCreateManifest(String buildImageTag, List manifestImages) {
    sh "docker manifest rm ${buildImageTag} || true"
    sh "docker manifest create ${buildImageTag} --insecure ${manifestImages.join(' ')}"
    sh "docker manifest push ${buildImageTag}"
}

/*
* Prepare the node for Docker multiplatform build
*
* Each element of the `mirrorRegistriesConfig` should contain:
*     - name: Name of the registry to mirror
*     - mirrors: List of mirrors for that registry, containing:
*         - url: mirror url
*         - insecure: whether the mirror is insecure
*/
void prepareForDockerMultiplatformBuild(List insecureRegistries = [], List mirrorRegistriesConfig = [], boolean debug = false) {
    cleanDockerMultiplatformBuild(debug)

    // For multiplatform build
    sh 'docker run --rm --privileged --name binfmt docker.io/tonistiigi/binfmt --install all'

    if (debug) { debugDockerMultiplatformBuild() }

    String buildkitdtomlConfig = "debug = ${debug}\n"

    insecureRegistries.each {
        buildkitdtomlConfig += "${getBuildkitRegistryConfigStr(it, true)}"
    }

    mirrorRegistriesConfig.each { mirrorRegistryCfg ->
        buildkitdtomlConfig += "[registry.\"${ mirrorRegistryCfg.name }\"]\n"
        buildkitdtomlConfig += "mirrors = [${ mirrorRegistryCfg.mirrors.collect { "\"${it.url }\"" }.join(',')}]\n"
        mirrorRegistryCfg.mirrors.each { mirror ->
            buildkitdtomlConfig += "${getBuildkitRegistryConfigStr(mirror.url, mirror.insecure)}"
        }
    }

    writeFile(file: 'buildkitd.toml', text: buildkitdtomlConfig)
    if (debug) {
        sh 'cat buildkitd.toml'
    }

    sh 'docker buildx create --name mybuilder --driver docker-container --driver-opt network=host --bootstrap --config ${WORKSPACE}/buildkitd.toml'
    sh 'docker buildx use mybuilder'

    if (debug) { debugDockerMultiplatformBuild() }
}

String getBuildkitRegistryConfigStr(String registryURL, boolean insecure) {
    return """[registry."${registryURL}"]
http = ${insecure}
"""
}

/*
* Return the mirror registry config for `docker.io`
*
* This checks for internal registry defined as env `DOCKER_REGISTRY_MIRROR`.
* Fallback to `mirror.gcr.io` is none defined.
*/
Map getDockerIOMirrorRegistryConfig() {
    return [
        name: 'docker.io',
        mirrors: [
            [
                url : env.DOCKER_REGISTRY_MIRROR ?: 'mirror.gcr.io',
                insecure: env.DOCKER_REGISTRY_MIRROR ? true : false,
            ]
        ],
    ]
}

/*
* Helpful commands to debug `docker buildx` preparation
*/
void debugDockerMultiplatformBuild() {
    sh 'docker context ls'
    sh 'docker buildx inspect'
    sh 'docker buildx ls'
}

/*
* Clean the node from Docker multiplatform configuration
*/
void cleanDockerMultiplatformBuild(boolean debug = false) {
    sh 'docker buildx rm mybuilder || true'
    sh 'docker rm -f binfmt || true'
    if (debug) { debugDockerMultiplatformBuild() }
}

//////////////////////////////////////////////////////////////////////////////////////
// Skopeo

/*
* Install skopeo CLI via yum
*
* https://github.com/containers/skopeo
*/
void installSkopeo() {
    cleanSkopeo()
    sh 'sudo yum -y install --nobest skopeo'
    sh 'skopeo --version'
}

/*
* Remove skopeo from the node
*/
void cleanSkopeo() {
    sh 'sudo yum -y remove skopeo || true'
}

void skopeoCopyRegistryImages(String oldImageName, String newImageName, int retries = 3) {
    sh "skopeo copy --retry-times ${retries} --tls-verify=false --all docker://${oldImageName} docker://${newImageName}"
}

/*
* Get reduced tag, aka X.Y, from the given tag
*/
String getReducedTag(String originalTag) {
    try {
        String[] versionSplit = originalTag.split("\\.")
        return "${versionSplit[0]}.${versionSplit[1]}"
    } catch (err) {
        println "[ERROR] ${originalTag} cannot be reduced to the format X.Y"
        throw err
    }
}
/*
* Update image description on quay.io
* descriptionString = string content that will be the description of the image
*/
void updateQuayImageDescription(String descriptionString, String namespace, String repository, Map credentials = [ 'token': '', 'usernamePassword': '' ]) {
    util.executeWithCredentialsMap(credentials) {
        def json = [
                description: descriptionString
        ]
        writeJSON(file: "description.json", json: json)
        archiveArtifacts(artifacts: 'description.json')
        sh(script: "curl -H 'Content-type: application/json' -H 'Authorization: Bearer ${QUAY_TOKEN}' -X PUT --data-binary '@description.json' https://quay.io/api/v1/repository/${namespace}/${repository}")
    }
}

