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
}
