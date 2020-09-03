import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class TreeBuildSpec extends JenkinsPipelineSpecification {
    def groovyScript = null
    def projectMap = ['projectA': ['goalA'], 'projectB': ['goalB1', 'goalB2'], 'projectC': ['goalC']]
    def projectCollection = ['projectA', 'projectB', 'projectC']

    def setup() {
        groovyScript = loadPipelineScriptForTest("vars/treebuild.groovy")
        explicitlyMockPipelineVariable("out")
    }

    def "[treebuild.groovy] build with map and skipTests"() {
        when:
        groovyScript.build(projectMap, 'settingsXmlId', true)
        then:
        1 * getPipelineMock('util.checkoutProjects')([['projectA', 'projectB', 'projectC'], 'projectC'])
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goalA', true)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalB1', true)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalB2', true)
        1 * getPipelineMock('util.buildProject')('projectC', 'settingsXmlId', 'goalC', true)
    }

    def "[treebuild.groovy] build with map without skipTests"() {
        when:
        groovyScript.build(projectMap, 'settingsXmlId')
        then:
        1 * getPipelineMock('util.checkoutProjects')([['projectA', 'projectB', 'projectC'], 'projectC'])
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goalA', null)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalB1', null)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalB2', null)
        1 * getPipelineMock('util.buildProject')('projectC', 'settingsXmlId', 'goalC', null)
    }

    def "[treebuild.groovy] build with collection with skipTests"() {
        when:
        groovyScript.build(projectCollection, 'settingsXmlId', 'goalX', false)
        then:
        1 * getPipelineMock('util.checkoutProjects')([['projectA', 'projectB', 'projectC'], 'projectC'])
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goalX', false)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalX', false)
        1 * getPipelineMock('util.buildProject')('projectC', 'settingsXmlId', 'goalX', false)
    }

    def "[treebuild.groovy] build with collection without skipTests"() {
        when:
        groovyScript.build(projectCollection, 'settingsXmlId', 'goalX')
        then:
        1 * getPipelineMock('util.checkoutProjects')([['projectA', 'projectB', 'projectC'], 'projectC'])
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goalX', null)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalX', null)
        1 * getPipelineMock('util.buildProject')('projectC', 'settingsXmlId', 'goalX', null)
    }

    def "[treebuild.groovy] upstreamBuild with map and skipTests"() {
        when:
        groovyScript.upstreamBuild(projectMap, 'projectB', 'settingsXmlId', true)
        then:
        1 * getPipelineMock('util.checkoutProjects')(['projectA', 'projectB', 'projectC'], 'projectB')
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goalA', true)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalB1', true)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalB2', true)
    }

    def "[treebuild.groovy] upstreamBuild with map without skipTests"() {
        when:
        groovyScript.upstreamBuild(projectMap, 'projectB', 'settingsXmlId')
        then:
        1 * getPipelineMock('util.checkoutProjects')(['projectA', 'projectB', 'projectC'], 'projectB')
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goalA', null)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalB1', null)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalB2', null)
    }

    def "[treebuild.groovy] upstreamBuild with collection with skipTests"() {
        when:
        groovyScript.upstreamBuild(projectCollection, 'projectB', 'settingsXmlId', 'goalX', false)
        then:
        1 * getPipelineMock('util.checkoutProjects')([['projectA', 'projectB', 'projectC'], 'projectB'])
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goalX', false)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalX', false)
    }

    def "[treebuild.groovy] upstreamBuild with collection without skipTests"() {
        when:
        groovyScript.upstreamBuild(projectCollection, 'projectB', 'settingsXmlId', 'goalX')
        then:
        1 * getPipelineMock('util.checkoutProjects')([['projectA', 'projectB', 'projectC'], 'projectB'])
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goalX', null)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalX', null)
    }

    def "[treebuild.groovy] upstreamBuild project not in project collection"() {
        when:
        groovyScript.upstreamBuild(projectMap, 'projectX', 'settingsXmlId', true)
        then:
        1 * getPipelineMock('util.checkoutProjects')(['projectA', 'projectB', 'projectC'], 'projectX')
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goalA', true)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalB1', true)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalB2', true)
        0 * getPipelineMock('util.buildProject')('projectX', _, _, _)
    }
}