import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class PncSpec extends JenkinsPipelineSpecification{
    Script groovyScript = null
    def env = [:]
    def pncVersionsResponse = null
    def pncBuildsResponse = null
    def pncApiUrl = null

    def setup() {
        groovyScript = loadPipelineScriptForTest('vars/pnc.groovy')
        explicitlyMockPipelineVariable("out")

        pncVersionsResponse = new File(getClass().getResource('/pnc-versions.json').toURI()).text
        pncBuildsResponse = new File(getClass().getResource('/pnc-builds-1912.json').toURI()).text

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

    def "[pnc.groovy] get pages number"() {
        setup:
        def productId = "155"
        def params = [q: ""]
        groovyScript.getBinding().setVariable("env", env)
        when:
        def result = groovyScript.getPagesNumber("products/${productId}/versions", params)
        then:
        1 * getPipelineMock( "sh" )( [returnStdout: true, script: "curl -s -X GET \"Accept: application/json\" \"${pncApiUrl}/products/${productId}/versions?pageIndex=0&pageSize=200&q=\""] ) >> pncVersionsResponse
        1 * getPipelineMock("util.serializeQueryParams")(params) >> { return "q="}
        expect:
        result == 1
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

    def "[pnc.groovy] get builds from milestone id"() {
        setup:
        def milestoneId = "1912"
        def projects = ["kiegroup/drools", "kiegroup/kogito-runtimes"]
        groovyScript.getBinding().setVariable("env", env)
        when:
        def result = groovyScript.getBuildsFromMilestoneId(milestoneId, projects)
        then:
        2 * getPipelineMock( "sh" )( [returnStdout: true, script: "curl -s -X GET \"Accept: application/json\" \"${pncApiUrl}/product-milestones/${milestoneId}/builds?pageIndex=0&pageSize=200&q=temporaryBuild==false\""] ) >> pncBuildsResponse
        2 * getPipelineMock("util.serializeQueryParams")([q: "temporaryBuild==false"]) >> { return "q=temporaryBuild==false"}
        expect:
        result.size() == 2
        result["kiegroup/drools"] == "8.27.0.Beta-redhat-00005"
        result["kiegroup/kogito-runtimes"] == "1.27.0.Final-redhat-00005"
    }
}
