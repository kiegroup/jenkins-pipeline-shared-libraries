import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class PncSpec extends JenkinsPipelineSpecification{
    Script groovyScript = null
    def env = [:]
    def pncVersionsResponse = null
    def pncApiUrl = null

    def setup() {
        groovyScript = loadPipelineScriptForTest('vars/pnc.groovy')
        explicitlyMockPipelineVariable("out")

        def url = getClass().getResource('/pnc-versions.txt')
        pncVersionsResponse = new File(url.toURI()).text

        // setup default env variables
        pncApiUrl = 'http://localhost:8080/pnc-rest/v2'
        env['PNC_API_URL'] = pncApiUrl
    }

    def "[pnc.groovy] query PNC api"() {
        setup:
        def productId = "155"
        def params = [q: ""]
        groovyScript.getBinding().setVariable("env", env)
        when:
        def result = groovyScript.queryPNC("products/${productId}/versions", params)
        then:
        1 * getPipelineMock( "sh" )( [returnStdout: true, script: "curl -s -X GET \"Accept: application/json\" \"${pncApiUrl}/products/${productId}/versions?pageIndex=0&pageSize=200&q=\""] ) >> pncVersionsResponse
        1 * getPipelineMock("util.serializeQueryParams")(params) >> { return "q="}
        expect:
        result.totalHits == 2
        result.content?.first()?.id == '364'
        result.content?.first()?.product?.id == '155'
        result.content?.first()?.product?.name == 'RHOSS-LOGIC'
    }

    def "[pnc.groovy] get milestone for product"() {
        setup:
        def productId = "155"
        groovyScript.getBinding().setVariable("env", env)
        when:
        def result = groovyScript.getAllMilestonesForProduct(productId)
        then:
        1 * getPipelineMock( "sh" )( [returnStdout: true, script: "curl -s -X GET \"Accept: application/json\" \"${pncApiUrl}/products/${productId}/versions?pageIndex=0&pageSize=200&q=\""] ) >> pncVersionsResponse
        1 * getPipelineMock("util.serializeQueryParams")([q: ""]) >> { return "q="}
        expect:
        result.totalHits == 2
        result.content?.first()?.id == '364'
        result.content?.first()?.product?.id == '155'
        result.content?.first()?.product?.name == 'RHOSS-LOGIC'
    }

    def "[pnc.groovy] get current milestone for product"() {
        setup:
        def productId = "155"
        groovyScript.getBinding().setVariable("env", env)
        when:
        def result = groovyScript.getCurrentMilestoneForProduct(productId)
        then:
        1 * getPipelineMock( "sh" )( [returnStdout: true, script: "curl -s -X GET \"Accept: application/json\" \"${pncApiUrl}/products/${productId}/versions?pageIndex=0&pageSize=200&q=\""] ) >> pncVersionsResponse
        1 * getPipelineMock("util.serializeQueryParams")([q: ""]) >> { return "q="}
        expect:
        result.version == "1.25.0.CR4"
        result.id == '1912'
    }

    def "[pnc.groovy] get milestone id"() {
        setup:
        def productId = "155"
        def milestone = "1.25.0.CR2"
        groovyScript.getBinding().setVariable("env", env)
        when:
        def result = groovyScript.getMilestoneId(productId, milestone)
        then:
        1 * getPipelineMock( "sh" )( [returnStdout: true, script: "curl -s -X GET \"Accept: application/json\" \"${pncApiUrl}/products/${productId}/versions?pageIndex=0&pageSize=200&q=\""] ) >> pncVersionsResponse
        1 * getPipelineMock("util.serializeQueryParams")([q: ""]) >> { return "q="}
        expect:
        result == "1891"
    }

    def "[pnc.groovy] get current milestone id"() {
        setup:
        def productId = "155"
        groovyScript.getBinding().setVariable("env", env)
        when:
        def result = groovyScript.getCurrentMilestoneId(productId)
        then:
        1 * getPipelineMock( "sh" )( [returnStdout: true, script: "curl -s -X GET \"Accept: application/json\" \"${pncApiUrl}/products/${productId}/versions?pageIndex=0&pageSize=200&q=\""] ) >> pncVersionsResponse
        1 * getPipelineMock("util.serializeQueryParams")([q: ""]) >> { return "q="}
        expect:
        result == "1912"
    }
}
