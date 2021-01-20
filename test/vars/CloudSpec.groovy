import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class CloudSpec extends JenkinsPipelineSpecification {
    def groovyScript = null
    def projectBranchMappingProperties = null


    def setup() {
        groovyScript = loadPipelineScriptForTest("vars/cloud.groovy")
    }

    def "[cloud.groovy] isQuayImagePublic returns true"() {
        setup:
        groovyScript.getBinding().setVariable("QUAY_TOKEN", 'quaytoken')
        when:
        def result = groovyScript.isQuayImagePublic('namespace', 'repository', 'TOKEN')
        then:
        1 * getPipelineMock('string.call')([credentialsId: 'TOKEN', variable: 'QUAY_TOKEN']) >> 'token'
        1 * getPipelineMock("withCredentials")(['token'], _ as Closure)
        1 * getPipelineMock("sh")([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'true'
        result
    }

    def "[cloud.groovy] isQuayImagePublic returns false"() {
        setup:
        groovyScript.getBinding().setVariable("QUAY_TOKEN", 'quaytoken')
        when:
        def result = groovyScript.isQuayImagePublic('namespace', 'repository', 'TOKEN')
        then:
        1 * getPipelineMock('string.call')([credentialsId: 'TOKEN', variable: 'QUAY_TOKEN']) >> 'token'
        1 * getPipelineMock("withCredentials")(['token'], _ as Closure)
        1 * getPipelineMock("sh")([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'false'
        !result
    }

    def "[cloud.groovy] isQuayImagePublic returns anything"() {
        setup:
        groovyScript.getBinding().setVariable("QUAY_TOKEN", 'quaytoken')
        when:
        def result = groovyScript.isQuayImagePublic('namespace', 'repository', 'TOKEN')
        then:
        1 * getPipelineMock('string.call')([credentialsId: 'TOKEN', variable: 'QUAY_TOKEN']) >> 'token'
        1 * getPipelineMock("withCredentials")(['token'], _ as Closure)
        1 * getPipelineMock("sh")([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'anything'
        !result
    }

    def "[cloud.groovy] setQuayImagePublic returns true"() {
        setup:
        groovyScript.getBinding().setVariable("QUAY_TOKEN", 'quaytoken')
        when:
        def result = groovyScript.setQuayImagePublic('namespace', 'repository', 'TOKEN')
        then:
        1 * getPipelineMock('string.call')([credentialsId: 'TOKEN', variable: 'QUAY_TOKEN']) >> 'token'
        1 * getPipelineMock("withCredentials")(['token'], _ as Closure)
        1 * getPipelineMock("sh")([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"]) >> 'true'
        result
    }

    def "[cloud.groovy] setQuayImagePublic returns false"() {
        setup:
        groovyScript.getBinding().setVariable("QUAY_TOKEN", 'quaytoken')
        when:
        def result = groovyScript.setQuayImagePublic('namespace', 'repository', 'TOKEN')
        then:
        1 * getPipelineMock('string.call')([credentialsId: 'TOKEN', variable: 'QUAY_TOKEN']) >> 'token'
        1 * getPipelineMock("withCredentials")(['token'], _ as Closure)
        1 * getPipelineMock("sh")([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"]) >> 'false'
        !result
    }

    def "[cloud.groovy] setQuayImagePublic curl returns anything"() {
        setup:
        groovyScript.getBinding().setVariable("QUAY_TOKEN", 'quaytoken')
        when:
        def result = groovyScript.setQuayImagePublic('namespace', 'repository', 'TOKEN')
        then:
        1 * getPipelineMock('string.call')([credentialsId: 'TOKEN', variable: 'QUAY_TOKEN']) >> 'token'
        1 * getPipelineMock("withCredentials")(['token'], _ as Closure)
        1 * getPipelineMock("sh")([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"]) >> 'anything'
        !result
    }

    def "[cloud.groovy] makeQuayImagePublic already public"() {
        setup:
        groovyScript.getBinding().setVariable("QUAY_TOKEN", 'quaytoken')
        when:
        groovyScript.makeQuayImagePublic('namespace', 'repository', 'TOKEN')
        then:
        1 * getPipelineMock('string.call')([credentialsId: 'TOKEN', variable: 'QUAY_TOKEN']) >> 'token'
        1 * getPipelineMock("withCredentials")(['token'], _ as Closure)
        1 * getPipelineMock("sh")([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'true'
        0 * getPipelineMock("sh")([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"]) >> 'anything'
    }

    def "[cloud.groovy] makeQuayImagePublic not yet public"() {
        setup:
        groovyScript.getBinding().setVariable("QUAY_TOKEN", 'quaytoken')
        when:
        groovyScript.makeQuayImagePublic('namespace', 'repository', 'TOKEN')
        then:
        2 * getPipelineMock('string.call')([credentialsId: 'TOKEN', variable: 'QUAY_TOKEN']) >> 'token'
        2 * getPipelineMock("withCredentials")(['token'], _ as Closure)
        1 * getPipelineMock("sh")([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'false'
        1 * getPipelineMock("sh")([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"]) >> 'true'
    }

    def "[cloud.groovy] makeQuayImagePublic raise error"() {
        setup:
        groovyScript.getBinding().setVariable("QUAY_TOKEN", 'quaytoken')
        when:
        groovyScript.makeQuayImagePublic('namespace', 'repository', 'TOKEN')
        then:
        2 * getPipelineMock('string.call')([credentialsId: 'TOKEN', variable: 'QUAY_TOKEN']) >> 'token'
        2 * getPipelineMock("withCredentials")(['token'], _ as Closure)
        1 * getPipelineMock("sh")([returnStdout: true, script: "curl -H 'Authorization: Bearer quaytoken' -X GET https://quay.io/api/v1/repository/namespace/repository | jq '.is_public'"]) >> 'false'
        1 * getPipelineMock("sh")([returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer quaytoken' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/namespace/repository/changevisibility | jq '.success'"]) >> 'false'
        1 * getPipelineMock("error").call("Cannot set image quay.io/namespace/repository as visible")
    }
}