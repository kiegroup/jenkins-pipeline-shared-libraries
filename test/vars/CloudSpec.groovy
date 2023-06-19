import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class CloudSpec extends JenkinsPipelineSpecification {

    def groovyScript = null
    def projectBranchMappingProperties = null

    def setup() {
        groovyScript = loadPipelineScriptForTest('vars/cloud.groovy')
        explicitlyMockPipelineVariable("out")
    }

    /////////////////////////////////////////////////////////////////////
    // isQuayImagePublic

    def "[cloud.groovy] isQuayImagePublic token returns true"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        def result = groovyScript.isQuayImagePublic('namespace', 'repository', [ token: 'TOKEN' ])
        then:
        1 * getPipelineMock('util.executeWithCredentialsMap')([token: 'TOKEN'], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'true'
        result
    }

    def "[cloud.groovy] isQuayImagePublic token returns false"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        def result = groovyScript.isQuayImagePublic('namespace', 'repository', [ token: 'TOKEN' ])
        then:
        1 * getPipelineMock('util.executeWithCredentialsMap')([token: 'TOKEN'], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'false'
        !result
    }

    def "[cloud.groovy] isQuayImagePublic token returns anything"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        def result = groovyScript.isQuayImagePublic('namespace', 'repository', [ token: 'TOKEN' ])
        then:
        1 * getPipelineMock('util.executeWithCredentialsMap')([token: 'TOKEN'], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'anything'
        !result
    }

    def "[cloud.groovy] isQuayImagePublic usernamePassword returns true"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        def result = groovyScript.isQuayImagePublic('namespace', 'repository', [ usernamePassword: 'USERNAME_PASSWORD' ])
        then:
        1 * getPipelineMock('util.executeWithCredentialsMap')([usernamePassword: 'USERNAME_PASSWORD'], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'true'
        result
    }

    def "[cloud.groovy] isQuayImagePublic usernamePassword returns false"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        def result = groovyScript.isQuayImagePublic('namespace', 'repository', [ usernamePassword: 'USERNAME_PASSWORD' ])
        then:
        1 * getPipelineMock('util.executeWithCredentialsMap')([usernamePassword: 'USERNAME_PASSWORD'], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'false'
        !result
    }

    def "[cloud.groovy] isQuayImagePublic usernamePassword returns anything"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        def result = groovyScript.isQuayImagePublic('namespace', 'repository', [ usernamePassword: 'USERNAME_PASSWORD' ])
        then:
        1 * getPipelineMock('util.executeWithCredentialsMap')([usernamePassword: 'USERNAME_PASSWORD'], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'anything'
        !result
    }

    def "[cloud.groovy] isQuayImagePublic no creds"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        def result = groovyScript.isQuayImagePublic('namespace', 'repository')
        then:
        1 * getPipelineMock('util.executeWithCredentialsMap')([ token: '', usernamePassword: '' ], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'anything'
        !result
    }

    /////////////////////////////////////////////////////////////////////
    // setQuayImagePublic

    def "[cloud.groovy] setQuayImagePublic token returns true"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        def result = groovyScript.setQuayImagePublic('namespace', 'repository', [ token: 'TOKEN' ])
        then:
        1 * getPipelineMock('util.executeWithCredentialsMap')([token: 'TOKEN'], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"]) >> 'true'
        result
    }

    def "[cloud.groovy] setQuayImagePublic token returns false"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        def result = groovyScript.setQuayImagePublic('namespace', 'repository', [ token: 'TOKEN' ])
        then:
        1 * getPipelineMock('util.executeWithCredentialsMap')([token: 'TOKEN'], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"]) >> 'false'
        !result
    }

    def "[cloud.groovy] setQuayImagePublic token curl returns anything"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        def result = groovyScript.setQuayImagePublic('namespace', 'repository', [ token: 'TOKEN' ])
        then:
        1 * getPipelineMock('util.executeWithCredentialsMap')([token: 'TOKEN'], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"]) >> 'anything'
        !result
    }

    def "[cloud.groovy] setQuayImagePublic usernamePassword returns true"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        def result = groovyScript.setQuayImagePublic('namespace', 'repository', [ usernamePassword: 'USERNAME_PASSWORD' ])
        then:
        1 * getPipelineMock('util.executeWithCredentialsMap')([usernamePassword: 'USERNAME_PASSWORD'], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"]) >> 'true'
        result
    }

    def "[cloud.groovy] setQuayImagePublic usernamePassword returns false"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        def result = groovyScript.setQuayImagePublic('namespace', 'repository', [ usernamePassword: 'USERNAME_PASSWORD' ])
        then:
        1 * getPipelineMock('util.executeWithCredentialsMap')([usernamePassword: 'USERNAME_PASSWORD'], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"]) >> 'false'
        !result
    }

    def "[cloud.groovy] setQuayImagePublic usernamePassword curl returns anything"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        def result = groovyScript.setQuayImagePublic('namespace', 'repository', [ usernamePassword: 'USERNAME_PASSWORD' ])
        then:
        1 * getPipelineMock('util.executeWithCredentialsMap')([usernamePassword: 'USERNAME_PASSWORD'], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"]) >> 'anything'
        !result
    }

    def "[cloud.groovy] setQuayImagePublic no creds"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        def result = groovyScript.setQuayImagePublic('namespace', 'repository')
        then:
        1 * getPipelineMock('util.executeWithCredentialsMap')([ token: '', usernamePassword: '' ], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"]) >> 'anything'
        !result
    }

    /////////////////////////////////////////////////////////////////////
    // makeQuayImagePublic

    def "[cloud.groovy] makeQuayImagePublic token already public"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        groovyScript.makeQuayImagePublic('namespace', 'repository', [ token: 'TOKEN' ])
        then:
        1 * getPipelineMock('util.executeWithCredentialsMap')([token: 'TOKEN'], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'true'
        0 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"]) >> 'anything'
    }

    def "[cloud.groovy] makeQuayImagePublic token not yet public"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        groovyScript.makeQuayImagePublic('namespace', 'repository', [ token: 'TOKEN' ])
        then:
        2 * getPipelineMock('util.executeWithCredentialsMap')([token: 'TOKEN'], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'false'
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"]) >> 'true'
    }

    def "[cloud.groovy] makeQuayImagePublic token raise error"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        groovyScript.makeQuayImagePublic('namespace', 'repository', [ token: 'TOKEN' ])
        then:
        2 * getPipelineMock('util.executeWithCredentialsMap')([token: 'TOKEN'], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'false'
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"]) >> 'false'
        1 * getPipelineMock('error').call('Cannot set image quay.io/namespace/repository as visible')
    }

    def "[cloud.groovy] makeQuayImagePublic usernamePassword already public"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        groovyScript.makeQuayImagePublic('namespace', 'repository', [ usernamePassword: 'USERNAME_PASSWORD' ])
        then:
        1 * getPipelineMock('util.executeWithCredentialsMap')([usernamePassword: 'USERNAME_PASSWORD'], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'true'
        0 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"]) >> 'anything'
    }

    def "[cloud.groovy] makeQuayImagePublic usernamePassword not yet public"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        groovyScript.makeQuayImagePublic('namespace', 'repository', [ usernamePassword: 'USERNAME_PASSWORD' ])
        then:
        2 * getPipelineMock('util.executeWithCredentialsMap')([usernamePassword: 'USERNAME_PASSWORD'], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'false'
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"]) >> 'true'
    }

    def "[cloud.groovy] makeQuayImagePublic usernamePassword raise error"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        groovyScript.makeQuayImagePublic('namespace', 'repository', [ usernamePassword: 'USERNAME_PASSWORD' ])
        then:
        2 * getPipelineMock('util.executeWithCredentialsMap')([usernamePassword: 'USERNAME_PASSWORD'], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'false'
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"]) >> 'false'
        1 * getPipelineMock('error').call('Cannot set image quay.io/namespace/repository as visible')
    }

    def "[cloud.groovy] makeQuayImagePublic no creds"() {
        setup:
        groovyScript.getBinding().setVariable('QUAY_TOKEN', 'quaytoken')
        when:
        groovyScript.makeQuayImagePublic('namespace', 'repository')
        then:
        1 * getPipelineMock('util.executeWithCredentialsMap')([ token: '', usernamePassword: ''], _)
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'true'
        0 * getPipelineMock('sh')([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"])
    }

    /////////////////////////////////////////////////////////////////////
    // cleanContainersAndImages

    def "[cloud.groovy] cleanContainersAndImages no container engine"() {
        when:
        groovyScript.cleanContainersAndImages()
        then:
        1 * getPipelineMock("sh")([script: "podman ps -a -q | tr '\\n' ','", returnStdout: true]) >> "one,two"
        1 * getPipelineMock("sh")("podman rm -f one || date")
        1 * getPipelineMock("sh")("podman rm -f two || date")
        1 * getPipelineMock("sh")([script: "podman images -q | tr '\\n' ','", returnStdout: true]) >> "hello,bonjour,hallo,ola"
        1 * getPipelineMock("sh")("podman rmi -f hello || date")
        1 * getPipelineMock("sh")("podman rmi -f bonjour || date")
        1 * getPipelineMock("sh")("podman rmi -f hallo || date")
        1 * getPipelineMock("sh")("podman rmi -f ola || date")
    }

    def "[cloud.groovy] cleanContainersAndImages with docker"() {
        when:
        groovyScript.cleanContainersAndImages('docker')
        then:
        1 * getPipelineMock("sh")([script: "docker ps -a -q | tr '\\n' ','", returnStdout: true]) >> "one,two"
        1 * getPipelineMock("sh")("docker rm -f one || date")
        1 * getPipelineMock("sh")("docker rm -f two || date")
        1 * getPipelineMock("sh")([script: "docker images -q | tr '\\n' ','", returnStdout: true]) >> "hello,bonjour,hallo,ola"
        1 * getPipelineMock("sh")("docker rmi -f hello || date")
        1 * getPipelineMock("sh")("docker rmi -f bonjour || date")
        1 * getPipelineMock("sh")("docker rmi -f hallo || date")
        1 * getPipelineMock("sh")("docker rmi -f ola || date")
    }

    def "[cloud.groovy] cleanContainersAndImages no containers/images"() {
        when:
        groovyScript.cleanContainersAndImages()
        then:
        1 * getPipelineMock("sh")([script: "podman ps -a -q | tr '\\n' ','", returnStdout: true]) >> ""
        0 * getPipelineMock("sh")("podman rm -f one || date")
        0 * getPipelineMock("sh")("podman rm -f  || date")
        1 * getPipelineMock("sh")([script: "podman images -q | tr '\\n' ','", returnStdout: true]) >> ""
        0 * getPipelineMock("sh")("podman rmi -f hello || date")
        0 * getPipelineMock("sh")("podman rmi -f  || date")
    }

    /////////////////////////////////////////////////////////////////////
    // startLocalRegistry & cleanLocalRegistry

    def "[cloud.groovy] cleanLocalRegistry default"() {
        when:
        groovyScript.cleanLocalRegistry()
        then:
        1 * getPipelineMock("sh")("docker rm -f registry-5000 || true")
    }

    def "[cloud.groovy] cleanLocalRegistry with port"() {
        when:
        groovyScript.cleanLocalRegistry(6986)
        then:
        1 * getPipelineMock("sh")("docker rm -f registry-6986 || true")
    }

    def "[cloud.groovy] startLocalRegistry default"() {
        when:
        def result = groovyScript.startLocalRegistry()
        then:
        1 * getPipelineMock("sh")("docker rm -f registry-5000 || true")
        1 * getPipelineMock("sh")("docker run -d -p 5000:5000 --restart=always --name registry-5000 registry:2")
        result == "localhost:5000"
    }

    def "[cloud.groovy] startLocalRegistry with port"() {
        when:
        def result = groovyScript.startLocalRegistry(63213)
        then:
        1 * getPipelineMock("sh")("docker rm -f registry-63213 || true")
        1 * getPipelineMock("sh")("docker run -d -p 63213:5000 --restart=always --name registry-63213 registry:2")
        result == "localhost:63213"
    }

    /////////////////////////////////////////////////////////////////////
    // installSkopeo & cleanSkopeo

    def "[cloud.groovy] installSkopeo default"() {
        when:
        groovyScript.installSkopeo()
        then:
        1 * getPipelineMock("sh")('sudo yum -y remove skopeo || true')
        1 * getPipelineMock("sh")('sudo yum -y install --nobest skopeo')
        1 * getPipelineMock("sh")('skopeo --version')
    }

    def "[cloud.groovy] cleanSkopeo default"() {
        when:
        groovyScript.cleanSkopeo()
        then:
        1 * getPipelineMock("sh")('sudo yum -y remove skopeo || true')
    }

    /////////////////////////////////////////////////////////////////////
    // skopeoCopyRegistryImages

    def "[cloud.groovy] skopeoCopyRegistryImages default"() {
        when:
        groovyScript.skopeoCopyRegistryImages('OLD_IMAGE', 'NEW_IMAGE')
        then:
        1 * getPipelineMock("sh")("skopeo copy --retry-times 3 --tls-verify=false --all docker://OLD_IMAGE docker://NEW_IMAGE")
    }

    def "[cloud.groovy] skopeoCopyRegistryImages default with 5 retries"() {
        when:
        groovyScript.skopeoCopyRegistryImages('OLD_IMAGE', 'NEW_IMAGE', 5)
        then:
        1 * getPipelineMock("sh")("skopeo copy --retry-times 5 --tls-verify=false --all docker://OLD_IMAGE docker://NEW_IMAGE")
    }

    /////////////////////////////////////////////////////////////////////
    // dockerSquashImage

    def "[cloud.groovy] dockerSquashImage default"() {
        when:
        def result = groovyScript.dockerSquashImage('BASE_IMAGE')
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: "docker history BASE_IMAGE | grep buildkit.dockerfile | wc -l"]) >> "6"
        1 * getPipelineMock("echo")("Got 7 layers to squash")
        1 * getPipelineMock("util.runWithPythonVirtualEnv")("docker-squash -v -m 'BASE_IMAGE squashed' -f 7 -t BASE_IMAGE BASE_IMAGE", 'cekit')
        1 * getPipelineMock("sh")("docker push BASE_IMAGE")
        result == 'BASE_IMAGE'
    }

    def "[cloud.groovy] dockerSquashImage with squash message"() {
        when:
        def result = groovyScript.dockerSquashImage('BASE_IMAGE', 'MESSAGE')
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: "docker history BASE_IMAGE | grep buildkit.dockerfile | wc -l"]) >> "6"
        1 * getPipelineMock("echo")("Got 7 layers to squash")
        1 * getPipelineMock("util.runWithPythonVirtualEnv")("docker-squash -v -m 'MESSAGE' -f 7 -t BASE_IMAGE BASE_IMAGE", 'cekit')
        1 * getPipelineMock("sh")("docker push BASE_IMAGE")
        result == 'BASE_IMAGE'
    }

    def "[cloud.groovy] dockerSquashImage with message and no replaceCurrentImage"() {
        when:
        def result = groovyScript.dockerSquashImage('BASE_IMAGE', 'MESSAGE', false)
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: "docker history BASE_IMAGE | grep buildkit.dockerfile | wc -l"]) >> "6"
        1 * getPipelineMock("echo")("Got 7 layers to squash")
        1 * getPipelineMock("util.runWithPythonVirtualEnv")("docker-squash -v -m 'MESSAGE' -f 7 -t BASE_IMAGE-squashed BASE_IMAGE", 'cekit')
        1 * getPipelineMock("sh")("docker push BASE_IMAGE-squashed")
        result == 'BASE_IMAGE-squashed'
    }

    /////////////////////////////////////////////////////////////////////
    // dockerDebugImage

    def "[cloud.groovy] dockerDebugImage default"() {
        when:
        groovyScript.dockerDebugImage('IMAGE')
        then:
        1 * getPipelineMock("sh")("docker images")
        1 * getPipelineMock("sh")("docker history IMAGE")
        1 * getPipelineMock("sh")("docker inspect IMAGE")
    }

    /////////////////////////////////////////////////////////////////////
    // dockerBuildPlatformImage

    def "[cloud.groovy] dockerBuildPlatformImage default"() {
        when:
        groovyScript.dockerBuildPlatformImage('IMAGE', 'PLATFORM')
        then:
        1 * getPipelineMock("sh")("docker buildx build --push --sbom=false --provenance=false --platform PLATFORM -t IMAGE .")
        1 * getPipelineMock("sh")("docker buildx imagetools inspect IMAGE")
        1 * getPipelineMock("sh")("docker pull --platform PLATFORM IMAGE")
    }

    /////////////////////////////////////////////////////////////////////
    // dockerCreateManifest

    def "[cloud.groovy] dockerCreateManifest default"() {
        when:
        groovyScript.dockerCreateManifest('IMAGE', [ 'IMAGE1', 'IMAGE2' ])
        then:
        1 * getPipelineMock("sh")("docker manifest rm IMAGE || true")
        1 * getPipelineMock("sh")("docker manifest create IMAGE --insecure IMAGE1 IMAGE2")
        1 * getPipelineMock("sh")("docker manifest push IMAGE")
    }

    /////////////////////////////////////////////////////////////////////
    // prepareForDockerMultiplatformBuild, debugDockerMultiplatformBuild & cleanDockerMultiplatformBuild

    def "[cloud.groovy] debugDockerMultiplatformBuild default"() {
        when:
        groovyScript.debugDockerMultiplatformBuild()
        then:
        1 * getPipelineMock("sh")("docker context ls")
        1 * getPipelineMock("sh")("docker buildx inspect")
        1 * getPipelineMock("sh")("docker buildx ls")
    }

    def "[cloud.groovy] cleanDockerMultiplatformBuild default"() {
        when:
        groovyScript.cleanDockerMultiplatformBuild()
        then:
        1 * getPipelineMock("sh")("docker buildx rm mybuilder || true")
        1 * getPipelineMock("sh")("docker rm -f binfmt || true")
        1 * getPipelineMock("sh")("docker context ls")
        1 * getPipelineMock("sh")("docker buildx inspect")
        1 * getPipelineMock("sh")("docker buildx ls")
    }

    def "[cloud.groovy] prepareForDockerMultiplatformBuild default"() {
        when:
        groovyScript.prepareForDockerMultiplatformBuild()
        then:
        1 * getPipelineMock("sh")('docker run --rm --privileged --name binfmt docker.io/tonistiigi/binfmt --install all')
        1 * getPipelineMock("writeFile")([file: 'buildkitd.toml', text: '''
debug = true
[registry."docker.io"]
mirrors = ["mirror.gcr.io"]
[registry."localhost:5000"]
http = true
        '''])
        1 * getPipelineMock("sh")("docker buildx rm mybuilder || true")
        1 * getPipelineMock("sh")("docker rm -f binfmt || true")
        1 * getPipelineMock("sh")("docker context ls")
        1 * getPipelineMock("sh")("docker buildx inspect")
        1 * getPipelineMock("sh")("docker buildx ls")
    }

    def "[cloud.groovy] prepareForDockerMultiplatformBuild with debug"() {
        when:
        groovyScript.prepareForDockerMultiplatformBuild(true)
        then:
        1 * getPipelineMock("sh")('docker run --rm --privileged --name binfmt docker.io/tonistiigi/binfmt --install all')
        1 * getPipelineMock("writeFile")([file: 'buildkitd.toml', text: '''
debug = true
[registry."docker.io"]
mirrors = ["mirror.gcr.io"]
[registry."localhost:5000"]
http = true
        '''])
        1 * getPipelineMock("sh")("docker buildx rm mybuilder || true")
        1 * getPipelineMock("sh")("docker rm -f binfmt || true")
        3 * getPipelineMock("sh")("docker context ls")
        3 * getPipelineMock("sh")("docker buildx inspect")
        3 * getPipelineMock("sh")("docker buildx ls")
    }

    /////////////////////////////////////////////////////////////////////
    // dockerBuildMultiPlatformImages

    def "[cloud.groovy] dockerBuildMultiPlatformImages default"() {
        when:
        groovyScript.dockerBuildMultiPlatformImages('IMAGE', ['linux/p1', 'windows/p2'])
        then:
        // First platform build
        1 * getPipelineMock("sh")("docker buildx build --push --sbom=false --provenance=false --platform linux/p1 -t IMAGE-linux-p1 .")
        1 * getPipelineMock("sh")("docker buildx imagetools inspect IMAGE-linux-p1")
        1 * getPipelineMock("sh")("docker pull --platform linux/p1 IMAGE-linux-p1")
        1 * getPipelineMock("sh")([returnStdout: true, script: "docker history IMAGE-linux-p1 | grep buildkit.dockerfile | wc -l"]) >> "6"
        1 * getPipelineMock("echo")("Got 7 layers to squash")
        1 * getPipelineMock("util.runWithPythonVirtualEnv")("docker-squash -v -m 'Squashed IMAGE' -f 7 -t IMAGE-linux-p1 IMAGE-linux-p1", 'cekit')
        1 * getPipelineMock("sh")("docker push IMAGE-linux-p1")

        // Second platform build
        1 * getPipelineMock("sh")("docker buildx build --push --sbom=false --provenance=false --platform windows/p2 -t IMAGE-windows-p2 .")
        1 * getPipelineMock("sh")("docker buildx imagetools inspect IMAGE-windows-p2")
        1 * getPipelineMock("sh")("docker pull --platform windows/p2 IMAGE-windows-p2")
        1 * getPipelineMock("sh")([returnStdout: true, script: "docker history IMAGE-windows-p2 | grep buildkit.dockerfile | wc -l"]) >> "6"
        1 * getPipelineMock("echo")("Got 7 layers to squash")
        1 * getPipelineMock("util.runWithPythonVirtualEnv")("docker-squash -v -m 'Squashed IMAGE' -f 7 -t IMAGE-windows-p2 IMAGE-windows-p2", 'cekit')
        1 * getPipelineMock("sh")("docker push IMAGE-windows-p2")

        // Manifest creation
        1 * getPipelineMock("sh")("docker manifest rm IMAGE || true")
        1 * getPipelineMock("sh")("docker manifest create IMAGE --insecure IMAGE-linux-p1 IMAGE-windows-p2")
        1 * getPipelineMock("sh")("docker manifest push IMAGE")
    }

    def "[cloud.groovy] dockerBuildMultiPlatformImages no squash"() {
        when:
        groovyScript.dockerBuildMultiPlatformImages('IMAGE', ['linux/p1', 'windows/p2'], false)
        then:
        // First platform build
        1 * getPipelineMock("sh")("docker buildx build --push --sbom=false --provenance=false --platform linux/p1 -t IMAGE-linux-p1 .")
        1 * getPipelineMock("sh")("docker buildx imagetools inspect IMAGE-linux-p1")
        1 * getPipelineMock("sh")("docker pull --platform linux/p1 IMAGE-linux-p1")
        0 * getPipelineMock("sh")([returnStdout: true, script: "docker history IMAGE-linux-p1 | grep buildkit.dockerfile | wc -l"]) >> "6"
        0 * getPipelineMock("echo")("Got 7 layers to squash")
        0 * getPipelineMock("util.runWithPythonVirtualEnv")("docker-squash -v -m 'Squashed IMAGE' -f 7 -t IMAGE-linux-p1 IMAGE-linux-p1", 'cekit')
        0 * getPipelineMock("sh")("docker push IMAGE-linux-p1")

        // Second platform build
        1 * getPipelineMock("sh")("docker buildx build --push --sbom=false --provenance=false --platform windows/p2 -t IMAGE-windows-p2 .")
        1 * getPipelineMock("sh")("docker buildx imagetools inspect IMAGE-windows-p2")
        1 * getPipelineMock("sh")("docker pull --platform windows/p2 IMAGE-windows-p2")
        0 * getPipelineMock("sh")([returnStdout: true, script: "docker history IMAGE-windows-p2 | grep buildkit.dockerfile | wc -l"]) >> "6"
        0 * getPipelineMock("echo")("Got 7 layers to squash")
        0 * getPipelineMock("util.runWithPythonVirtualEnv")("docker-squash -v -m 'Squashed IMAGE' -f 7 -t IMAGE-windows-p2 IMAGE-windows-p2", 'cekit')
        0 * getPipelineMock("sh")("docker push IMAGE-windows-p2")

        // Manifest creation
        1 * getPipelineMock("sh")("docker manifest rm IMAGE || true")
        1 * getPipelineMock("sh")("docker manifest create IMAGE --insecure IMAGE-linux-p1 IMAGE-windows-p2")
        1 * getPipelineMock("sh")("docker manifest push IMAGE")
    }

    def "[cloud.groovy] dockerBuildMultiPlatformImages with squash and message"() {
        when:
        groovyScript.dockerBuildMultiPlatformImages('IMAGE', ['linux/p1', 'windows/p2'], true, 'MESSAGE')
        then:
        // First platform build
        1 * getPipelineMock("sh")("docker buildx build --push --sbom=false --provenance=false --platform linux/p1 -t IMAGE-linux-p1 .")
        1 * getPipelineMock("sh")("docker buildx imagetools inspect IMAGE-linux-p1")
        1 * getPipelineMock("sh")("docker pull --platform linux/p1 IMAGE-linux-p1")
        1 * getPipelineMock("sh")([returnStdout: true, script: "docker history IMAGE-linux-p1 | grep buildkit.dockerfile | wc -l"]) >> "6"
        1 * getPipelineMock("echo")("Got 7 layers to squash")
        1 * getPipelineMock("util.runWithPythonVirtualEnv")("docker-squash -v -m 'MESSAGE' -f 7 -t IMAGE-linux-p1 IMAGE-linux-p1", 'cekit')
        1 * getPipelineMock("sh")("docker push IMAGE-linux-p1")

        // Second platform build
        1 * getPipelineMock("sh")("docker buildx build --push --sbom=false --provenance=false --platform windows/p2 -t IMAGE-windows-p2 .")
        1 * getPipelineMock("sh")("docker buildx imagetools inspect IMAGE-windows-p2")
        1 * getPipelineMock("sh")("docker pull --platform windows/p2 IMAGE-windows-p2")
        1 * getPipelineMock("sh")([returnStdout: true, script: "docker history IMAGE-windows-p2 | grep buildkit.dockerfile | wc -l"]) >> "6"
        1 * getPipelineMock("echo")("Got 7 layers to squash")
        1 * getPipelineMock("util.runWithPythonVirtualEnv")("docker-squash -v -m 'MESSAGE' -f 7 -t IMAGE-windows-p2 IMAGE-windows-p2", 'cekit')
        1 * getPipelineMock("sh")("docker push IMAGE-windows-p2")

        // Manifest creation
        1 * getPipelineMock("sh")("docker manifest rm IMAGE || true")
        1 * getPipelineMock("sh")("docker manifest create IMAGE --insecure IMAGE-linux-p1 IMAGE-windows-p2")
        1 * getPipelineMock("sh")("docker manifest push IMAGE")
    }

    def "[cloud.groovy] dockerBuildMultiPlatformImages with squash and message and debug"() {
        when:
        groovyScript.dockerBuildMultiPlatformImages('IMAGE', ['linux/p1', 'windows/p2'], true, 'MESSAGE', true)
        then:
        // First platform build
        1 * getPipelineMock("sh")("docker buildx build --push --sbom=false --provenance=false --platform linux/p1 -t IMAGE-linux-p1 .")
        1 * getPipelineMock("sh")("docker buildx imagetools inspect IMAGE-linux-p1")
        1 * getPipelineMock("sh")("docker pull --platform linux/p1 IMAGE-linux-p1")
        1 * getPipelineMock("sh")([returnStdout: true, script: "docker history IMAGE-linux-p1 | grep buildkit.dockerfile | wc -l"]) >> "6"
        1 * getPipelineMock("echo")("Got 7 layers to squash")
        1 * getPipelineMock("util.runWithPythonVirtualEnv")("docker-squash -v -m 'MESSAGE' -f 7 -t IMAGE-linux-p1 IMAGE-linux-p1", 'cekit')
        1 * getPipelineMock("sh")("docker push IMAGE-linux-p1")

        // Second platform build
        1 * getPipelineMock("sh")("docker buildx build --push --sbom=false --provenance=false --platform windows/p2 -t IMAGE-windows-p2 .")
        1 * getPipelineMock("sh")("docker buildx imagetools inspect IMAGE-windows-p2")
        1 * getPipelineMock("sh")("docker pull --platform windows/p2 IMAGE-windows-p2")
        1 * getPipelineMock("sh")([returnStdout: true, script: "docker history IMAGE-windows-p2 | grep buildkit.dockerfile | wc -l"]) >> "6"
        1 * getPipelineMock("echo")("Got 7 layers to squash")
        1 * getPipelineMock("util.runWithPythonVirtualEnv")("docker-squash -v -m 'MESSAGE' -f 7 -t IMAGE-windows-p2 IMAGE-windows-p2", 'cekit')
        1 * getPipelineMock("sh")("docker push IMAGE-windows-p2")

        // Manifest creation
        1 * getPipelineMock("sh")("docker manifest rm IMAGE || true")
        1 * getPipelineMock("sh")("docker manifest create IMAGE --insecure IMAGE-linux-p1 IMAGE-windows-p2")
        1 * getPipelineMock("sh")("docker manifest push IMAGE")

        // Debug commands
        5 * getPipelineMock("sh")("docker images")
        1 * getPipelineMock("sh")("docker history IMAGE")
        2 * getPipelineMock("sh")("docker history IMAGE-linux-p1")
        2 * getPipelineMock("sh")("docker history IMAGE-windows-p2")
        1 * getPipelineMock("sh")("docker inspect IMAGE")
        2 * getPipelineMock("sh")("docker inspect IMAGE-linux-p1")
        2 * getPipelineMock("sh")("docker inspect IMAGE-windows-p2")
    }

    /////////////////////////////////////////////////////////////////////
    // loginOpenShift

    def "[cloud.groovy] loginOpenShift default"() {
        setup:
        groovyScript.getBinding().setVariable("OC_USER", 'user')
        groovyScript.getBinding().setVariable("OC_PWD", 'password')
        when:
        groovyScript.loginOpenShift('OPENSHIFT_API', 'OPENSHIFT_CREDS_ID')
        then:
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'OPENSHIFT_CREDS_ID', usernameVariable: 'OC_USER', passwordVariable: 'OC_PWD']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")("oc login --username=user --password=password --server=OPENSHIFT_API --insecure-skip-tls-verify")
    }

    /////////////////////////////////////////////////////////////////////
    // loginOpenShiftRegistry

    def "[cloud.groovy] getOpenShiftRegistryURL default"() {
        when:
        def result = groovyScript.getOpenShiftRegistryURL()
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: "oc get routes -n openshift-image-registry | tail -1 | awk '{print \$2}'"]) >> 'OPENSHIFT_URL'
        result == 'OPENSHIFT_URL'
    }

    /////////////////////////////////////////////////////////////////////
    // loginOpenShiftRegistry

    def "[cloud.groovy] loginOpenShiftRegistry default"() {
        when:
        groovyScript.loginOpenShiftRegistry()
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: "oc get routes -n openshift-image-registry | tail -1 | awk '{print \$2}'"]) >> 'OPENSHIFT_URL'
        1 * getPipelineMock("sh")("set +x && docker login -u anything -p \$(oc whoami -t)  OPENSHIFT_URL")
    }

    def "[cloud.groovy] loginOpenShiftRegistry with container engine and options"() {
        when:
        groovyScript.loginOpenShiftRegistry('podman', '--tls-verify=false')
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: "oc get routes -n openshift-image-registry | tail -1 | awk '{print \$2}'"]) >> 'OPENSHIFT_URL'
        1 * getPipelineMock("sh")("set +x && podman login -u anything -p \$(oc whoami -t) --tls-verify=false OPENSHIFT_URL")
    }

    /////////////////////////////////////////////////////////////////////
    // loginContainerRegistry

    def "[cloud.groovy] loginContainerRegistry default"() {
        setup:
        groovyScript.getBinding().setVariable("REGISTRY_USER", 'user')
        groovyScript.getBinding().setVariable("REGISTRY_PWD", 'password')
        when:
        groovyScript.loginContainerRegistry('REGISTRY', 'REGISTRY_CREDS_ID')
        then:
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'REGISTRY_CREDS_ID', usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PWD']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")("set +x && docker login -u user -p password  REGISTRY")
    }

    def "[cloud.groovy] loginContainerRegistry with container engine and options"() {
        setup:
        groovyScript.getBinding().setVariable("REGISTRY_USER", 'user')
        groovyScript.getBinding().setVariable("REGISTRY_PWD", 'password')
        when:
        groovyScript.loginContainerRegistry('REGISTRY', 'REGISTRY_CREDS_ID', 'podman', '--tls-verify=false')
        then:
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'REGISTRY_CREDS_ID', usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PWD']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")("set +x && podman login -u user -p password --tls-verify=false REGISTRY")
    }
    
    /////////////////////////////////////////////////////////////////////
    // getReducedTag

    def "[cloud.groovy] getReducedTag with correct tag"() {
        when:
        def result = groovyScript.getReducedTag('1.36.0')
        then:
        0 * getPipelineMock("echo")("[INFO] 1.36.0 cannot be reduced to the format X.Y")
        result == "1.36"
    }

    def "[cloud.groovy] getReducedTag with incorrect tag"() {
        when:
        def result = groovyScript.getReducedTag('ANY_TAG')
        then:
        1 * getPipelineMock("echo")("[INFO] ANY_TAG cannot be reduced to the format X.Y")
        result == ''
    }
}
