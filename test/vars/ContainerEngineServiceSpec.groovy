import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import org.jenkinsci.plugins.workflow.steps.Step
import org.jenkinsci.plugins.workflow.steps.StepContext
import org.jenkinsci.plugins.workflow.steps.StepExecution
import org.kie.jenkins.ContainerEngineService

class ContainerEngineServiceSpec extends JenkinsPipelineSpecification {

    def steps
    def env = [:]

    def setup() {
        steps = new Step() {

            @Override
            StepExecution start(StepContext stepContext) throws Exception {
                return null
            }

        }
        steps.env = [:]
    }

    def "[ContainerEngineService.groovy] init default no env"() {
        when:
        def containerEngine = new ContainerEngineService(steps)
        then:
        containerEngine.containerEngine == 'docker'
        containerEngine.containerEngineTlsOptions == ''
    }

    def "[ContainerEngineService.groovy] init default with env"() {
        setup:
        steps.env.putAll([ 'CONTAINER_ENGINE': 'podman', 'CONTAINER_ENGINE_TLS_OPTIONS': '--tls-verify=false' ])
        when:
        def containerEngine = new ContainerEngineService(steps)
        then:
        containerEngine.containerEngine == 'podman'
        containerEngine.containerEngineTlsOptions == '--tls-verify=false'
    }

    def "[ContainerEngineService.groovy] init with container engine"() {
        when:
        def containerEngine = new ContainerEngineService(steps, 'podman', '--tls-verify=false')
        then:
        containerEngine.containerEngine == 'podman'
        containerEngine.containerEngineTlsOptions == '--tls-verify=false'
    }

    /////////////////////////////////////////////////////////////////////
    // pull, tag and push images

    def "[ContainerEngineService.groovy] pullImage default"() {
        when:
        new ContainerEngineService(steps).pullImage('IMAGE')
        then:
        1 * getPipelineMock('retry')(3, _)
        1 * getPipelineMock("sh")("docker pull  IMAGE")
    }

    def "[ContainerEngineService.groovy] pullImage with retries and container engine"() {
        when:
        new ContainerEngineService(steps, 'podman', '--tls-verify=false').pullImage('IMAGE', 1)
        then:
        1 * getPipelineMock('retry')(1, _)
        1 * getPipelineMock("sh")("podman pull --tls-verify=false IMAGE")
    }

    def "[ContainerEngineService.groovy] pushImage default"() {
        when:
        new ContainerEngineService(steps).pushImage('IMAGE')
        then:
        1 * getPipelineMock('retry')(3, _)
        1 * getPipelineMock("sh")("docker push  IMAGE")
    }

    def "[ContainerEngineService.groovy] pushImage with retries and container engine"() {
        when:
        new ContainerEngineService(steps, 'podman', '--tls-verify=false').pushImage('IMAGE', 1)
        then:
        1 * getPipelineMock('retry')(1, _)
        1 * getPipelineMock("sh")("podman push --tls-verify=false IMAGE")
    }

    def "[ContainerEngineService.groovy] tagImage default"() {
        when:
        new ContainerEngineService(steps).tagImage('OLD', 'NEW')
        then:
        1 * getPipelineMock("sh")("docker tag OLD NEW")
    }

    def "[ContainerEngineService.groovy] tagImage with container engine"() {
        when:
        new ContainerEngineService(steps, 'podman').tagImage('OLD', 'NEW')
        then:
        1 * getPipelineMock("sh")("podman tag OLD NEW")
    }

    /////////////////////////////////////////////////////////////////////
    // loginOpenShiftRegistry

    def "[ContainerEngineService.groovy] loginOpenShiftRegistry default"() {
        when:
        new ContainerEngineService(steps).loginOpenShiftRegistry()
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: "oc get routes -n openshift-image-registry | tail -1 | awk '{print \$2}'"]) >> 'OPENSHIFT_URL'
        1 * getPipelineMock("sh")("set +x && docker login -u anything -p \$(oc whoami -t)  OPENSHIFT_URL")
    }

    def "[ContainerEngineService.groovy] loginOpenShiftRegistry with container engine and options"() {
        when:
        new ContainerEngineService(steps, 'podman', '--tls-verify=false').loginOpenShiftRegistry()
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: "oc get routes -n openshift-image-registry | tail -1 | awk '{print \$2}'"]) >> 'OPENSHIFT_URL'
        1 * getPipelineMock("sh")("set +x && podman login -u anything -p \$(oc whoami -t) --tls-verify=false OPENSHIFT_URL")
    }

    /////////////////////////////////////////////////////////////////////
    // loginContainerRegistry

    def "[ContainerEngineService.groovy] loginContainerRegistry default"() {
        setup:
        steps.env.put("REGISTRY_USER", 'user')
        steps.env.put("REGISTRY_PWD", 'password')
        when:
        new ContainerEngineService(steps).loginContainerRegistry('REGISTRY', 'REGISTRY_CREDS_ID')
        then:
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'REGISTRY_CREDS_ID', usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PWD']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")("set +x && docker login -u user -p password  REGISTRY")
    }

    def "[ContainerEngineService.groovy] loginContainerRegistry with container engine and options"() {
        setup:
        steps.env.put("REGISTRY_USER", 'user')
        steps.env.put("REGISTRY_PWD", 'password')
        when:
        new ContainerEngineService(steps, 'podman', '--tls-verify=false').loginContainerRegistry('REGISTRY', 'REGISTRY_CREDS_ID')
        then:
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'REGISTRY_CREDS_ID', usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PWD']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")("set +x && podman login -u user -p password --tls-verify=false REGISTRY")
    }

    /////////////////////////////////////////////////////////////////////
    // cleanContainersAndImages

    def "[ContainerEngineService.groovy] clean no container engine"() {
        when:
        new ContainerEngineService(steps).clean()
        then:
        1 * getPipelineMock("sh")("docker system prune -a -f")
    }

    def "[ContainerEngineService.groovy] cleanContainersAndImages with podman"() {
        when:
        new ContainerEngineService(steps, 'podman').clean()
        then:
        1 * getPipelineMock("sh")("podman system prune -a -f")
    }

    /////////////////////////////////////////////////////////////////////
    // startLocalRegistry & cleanLocalRegistry

    def "[ContainerEngineService.groovy] cleanLocalRegistry default"() {
        when:
        new ContainerEngineService(steps).cleanLocalRegistry()
        then:
        1 * getPipelineMock("sh")("docker rm -f registry-5000 || true")
    }

    def "[ContainerEngineService.groovy] cleanLocalRegistry with port"() {
        when:
        new ContainerEngineService(steps).cleanLocalRegistry(6986)
        then:
        1 * getPipelineMock("sh")("docker rm -f registry-6986 || true")
    }

    def "[ContainerEngineService.groovy] cleanLocalRegistry with container engine"() {
        when:
        new ContainerEngineService(steps, 'podman').cleanLocalRegistry()
        then:
        1 * getPipelineMock("sh")("podman rm -f registry-5000 || true")
    }

    def "[ContainerEngineService.groovy] startLocalRegistry default"() {
        when:
        def result = new ContainerEngineService(steps).startLocalRegistry()
        then:
        1 * getPipelineMock("sh")("docker rm -f registry-5000 || true")
        1 * getPipelineMock("sh")("docker run -d -p 5000:5000 --restart=always --name registry-5000 registry:2")
        result == "localhost:5000"
    }

    def "[ContainerEngineService.groovy] startLocalRegistry with port"() {
        when:
        def result = new ContainerEngineService(steps).startLocalRegistry(63213)
        then:
        1 * getPipelineMock("sh")("docker rm -f registry-63213 || true")
        1 * getPipelineMock("sh")("docker run -d -p 63213:5000 --restart=always --name registry-63213 registry:2")
        result == "localhost:63213"
    }

    def "[ContainerEngineService.groovy] startLocalRegistry with container engine"() {
        when:
        def result = new ContainerEngineService(steps, 'podman').startLocalRegistry()
        then:
        1 * getPipelineMock("sh")("podman rm -f registry-5000 || true")
        1 * getPipelineMock("sh")("podman run -d -p 5000:5000 --restart=always --name registry-5000 registry:2")
        result == "localhost:5000"
    }
}
