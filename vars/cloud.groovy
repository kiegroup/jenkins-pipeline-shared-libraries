import org.kie.jenkins.ContainerEngineService

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
* Login to given OpenShift API string Credendials
*/
void loginOpenShiftFromAPICreds(String openshiftAPIKey, String openShiftCredsId) {
    withCredentials([string(credentialsId: openshiftAPIKey, variable: 'OPENSHIFT_API')]) {
        loginOpenShift(env.OPENSHIFT_API, openShiftCredsId)
    }
}

/*
* Login to current OpenShift registry
*
* It considers that you are already authenticated to OpenShift
*/
void loginOpenShiftRegistry(String containerEngine = 'docker', String containerEngineTlsOptions = '') {
    new ContainerEngineService(this, containerEngine, containerEngineTlsOptions).loginOpenShiftRegistry()
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
    new ContainerEngineService(this, containerEngine, containerEngineTlsOptions).loginContainerRegistry(registry, credsId)
}

void pullImage(String imageTag, int retries = 3, String containerEngine = 'docker', String containerEngineTlsOptions = '') {
    new ContainerEngineService(this, containerEngine, containerEngineTlsOptions).pullImage(imageTag, retries)
}

void pushImage(String imageTag, int retries = 3, String containerEngine = 'docker', String containerEngineTlsOptions = '') {
    new ContainerEngineService(this, containerEngine, containerEngineTlsOptions).pushImage(imageTag, retries)
}

void tagImage(String oldImageTag, String newImageTag, String containerEngine = 'docker') {
    new ContainerEngineService(this, containerEngine).tagImage(oldImageTag, newImageTag)
}

/*
* Cleanup all containers and images
*/
void cleanContainersAndImages(String containerEngine = 'podman') {
    new ContainerEngineService(this, containerEngine).clean()
}

/*
* Start local docker registry
*
* Accessible on `localhost:${port}`. Default port is 5000.
*/
String startLocalRegistry(int port = 5000) {
    return new ContainerEngineService(this).startLocalRegistry(port)
}

/*
* Clean local registry
*/
void cleanLocalRegistry(int port = 5000) {
    new ContainerEngineService(this).cleanLocalRegistry(port)
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
void dockerBuildMultiPlatformImages(String buildImageTag, List platforms, boolean squashImages = true, String squashMessage = "Squashed ${buildImageTag}", boolean debug = false) {
    // Build image locally in tgz file
    List buildPlatformImages = platforms.collect { platform ->
        String os_arch = platform.replaceAll('/', '-')
        String platformImage = "${buildImageTag}-${os_arch}"
        String finalPlatformImage = platformImage

        // Build
        dockerBuildPlatformImage(platformImage, platform)
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
    sh "docker buildx build --push --sbom=false --provenance=false --platform ${platform} -t ${buildImageTag} ."
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
    cleanDockerMultiplatformBuild()

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
void cleanDockerMultiplatformBuild() {
    sh 'docker buildx rm mybuilder || true'
    sh 'docker rm -f binfmt || true'
    debugDockerMultiplatformBuild()
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
