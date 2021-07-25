import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class JenkinsfileNightlySpec extends JenkinsPipelineSpecification {

    def groovyScript = null

    def setup() {
        groovyScript = loadPipelineScriptForTest("nightlyMethods.groovy")
    }

    def "additionalVariablesBranched7 current branch not main"() {
        setup:
        def env = [:]
        env['BRANCH_NAME'] = 'branchX'
        groovyScript.getBinding().setVariable("env", env)
        when:
        Map<String, String> result = groovyScript.additionalVariablesBranched7()
        then:
        0 * getPipelineMock('readFile')(_)
        result.size() == 0
    }

    def "additionalVariablesBranched7 branch main"() {
        setup:
        def fileName = '/branched-7-repository-list.txt'
        def filePath = "./script${fileName}"
        def url = getClass().getResource(fileName)
        def fileContent = new File(url.toURI()).text

        def env = [:]
        env['BRANCH_NAME'] = 'main'
        groovyScript.getBinding().setVariable("env", env)
        when:
        Map<String, String> result = groovyScript.additionalVariablesBranched7()
        then:
        1 * getPipelineMock('readFile')(filePath) >> { return fileContent}
        assert result.size() == 5
        assert result['kie-soup-scmRevision'] == '7.x'
        assert result['appformer-scmRevision'] == '7.x'
        assert result['droolsjbpm-knowledge-scmRevision'] == '7.x'
        assert result['drools-scmRevision'] == '7.x'
        assert result['optaplanner-scmRevision'] == '7.x'
    }

    def "additionalVariablesBranched7 branch main branched repository list"() {
        setup:
        def fileName = '/branched-7-repository-list-empty.txt'
        def filePath = "./script${fileName}"
        def url = getClass().getResource(fileName)
        def fileContent = new File(url.toURI()).text

        def env = [:]
        env['BRANCH_NAME'] = 'main'
        groovyScript.getBinding().setVariable("env", env)
        when:
        Map<String, String> result = groovyScript.additionalVariablesBranched7(filePath)
        then:
        1 * getPipelineMock('readFile')(filePath) >> { return fileContent}
        assert result.size() == 0
    }
}
