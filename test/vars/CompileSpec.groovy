import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class CompileSpec extends JenkinsPipelineSpecification {
    def groovyScript = null

    def setup() {
        groovyScript = loadPipelineScriptForTest("vars/compile.groovy")
        explicitlyMockPipelineVariable("out")
    }

    def "[compile.groovy] build"() {
        setup:
        def projectCollection = ['projectA', 'projectB', 'projectC']
        when:
        groovyScript.build(projectCollection, 'projectB', 'settingsXmlId', 'propertiesFileId')
        then:
        1 * getPipelineMock('util.getGoals')('projectA', 'propertiesFileId', 'upstream') >> { return 'goals upstream' }
        1 * getPipelineMock('util.getGoals')('projectB', 'propertiesFileId', 'current') >> { return 'goals current' }
        1 * getPipelineMock('util.getGoals')('projectC', 'propertiesFileId', 'downstream') >> { return 'goals downstream' }
        1 * getPipelineMock('util.checkoutProjects')(projectCollection)
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goals upstream')
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goals current')
        1 * getPipelineMock('util.buildProject')('projectC', 'settingsXmlId', 'goals downstream')
    }
}