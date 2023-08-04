package org.kie.jenkins

class ContainerEngineService {

    def steps

    String containerEngine
    String containerEngineTlsOptions

    ContainerEngineService(steps) {
        this(steps, steps.env['CONTAINER_ENGINE'], steps.env['CONTAINER_ENGINE_TLS_OPTIONS'])
    }

    ContainerEngineService(steps, String containerEngine) {
        this(steps, containerEngine, '')
    }

    ContainerEngineService(steps, String containerEngine, String containerEngineTlsOptions) {
        this.steps = steps
        this.containerEngine = containerEngine ?: 'docker'
        this.containerEngineTlsOptions = containerEngineTlsOptions ?: ''
    }

    void pullImage(String imageTag, int retries = 3) {
        steps.retry(retries) {
            steps.sh("${containerEngine} pull ${containerEngineTlsOptions} ${imageTag}")
        }
    }

    void pushImage(String imageTag, int retries = 3) {
        steps.retry(retries) {
            steps.sh("${containerEngine} push ${containerEngineTlsOptions} ${imageTag}")
        }
    }

    void tagImage(String oldImageTag, String newImageTag) {
        steps.sh("${containerEngine} tag ${oldImageTag} ${newImageTag}")
    }

    /*
    * Login to a container registry
    */
    void loginContainerRegistry(String registry, String credsId) {
        steps.withCredentials([steps.usernamePassword(credentialsId: credsId, usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PWD')]) {
            steps.sh("set +x && ${containerEngine} login -u ${steps.env['REGISTRY_USER']} -p ${steps.env['REGISTRY_PWD']} ${containerEngineTlsOptions} ${registry}")
        }
    }

    /*
    * Login to current OpenShift registry
    *
    * It considers that you are already authenticated to OpenShift
    */
    void loginOpenShiftRegistry() {
        // username can be anything. See https://docs.openshift.com/container-platform/4.4/registry/accessing-the-registry.html#registry-accessing-directly_accessing-the-registry
        steps.sh("set +x && ${containerEngine} login -u anything -p \$(oc whoami -t) ${containerEngineTlsOptions} ${getOpenShiftRegistryURL()}")
    }

    private String getOpenShiftRegistryURL() {
        return steps.sh(returnStdout: true, script: "oc get routes -n openshift-image-registry | tail -1 | awk '{print \$2}'")?.trim()
    }

    /*
    * Cleanup all containers and images
    */
    void clean() {
        steps.println '[INFO] Cleaning up running containers and images. Any error here can be ignored'
        steps.sh("${containerEngine} system prune -a -f")
    }

    /*
    * Start local docker registry
    *
    * Accessible on `localhost:${port}`. Default port is 5000.
    */
    String startLocalRegistry(int port = 5000) {
        cleanLocalRegistry(port)
        steps.sh("${containerEngine} run -d -p ${port}:5000 --restart=always --name registry-${port} registry:2")
        steps.sh("${containerEngine} ps")
        return "localhost:${port}"
    }

    /*
    * Clean local registry
    */
    void cleanLocalRegistry(int port = 5000) {
        steps.sh("${containerEngine} rm -f registry-${port} || true")
        steps.sh("${containerEngine} ps")
    }
}
