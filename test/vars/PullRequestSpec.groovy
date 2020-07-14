import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class PullRequestSpec extends JenkinsPipelineSpecification {
    def groovyScript = null
    def projectCollection = ['projectA', 'projectB', 'projectC']

    def setup() {
        groovyScript = loadPipelineScriptForTest("vars/pullrequest.groovy")
        explicitlyMockPipelineVariable("out")
        groovyScript.metaClass.WORKSPACE = '/'
    }

    def "[pullrequest.groovy] build  first project from collection"() {
        when:
        groovyScript.build(projectCollection, 'projectA', 'settingsXmlId', 'propertiesFileId', 'sonarCloudId')
        then:
        1 * getPipelineMock('util.checkoutProjects')(projectCollection, 'projectA')
        0 * getPipelineMock('util.getGoals')('projectA', 'propertiesFileId', 'upstream')
        1 * getPipelineMock('util.getGoals')('projectA', 'propertiesFileId') >> { return 'goals current' }
        0 * getPipelineMock('util.getGoals')('projectB', 'propertiesFileId', _)
        0 * getPipelineMock('util.getGoals')('projectC', 'propertiesFileId', _)
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goals current')
        0 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', _)
        0 * getPipelineMock('util.buildProject')('projectC', 'settingsXmlId', _)
        0 * getPipelineMock('maven.runMavenWithSettingsSonar')(_)
    }

    def "[pullrequest.groovy] build without sonarCloudReps"() {
        when:
        groovyScript.build(projectCollection, 'projectB', 'settingsXmlId', 'propertiesFileId', 'sonarCloudId')
        then:
        1 * getPipelineMock('util.checkoutProjects')(projectCollection, 'projectB')
        1 * getPipelineMock('util.getGoals')('projectA', 'propertiesFileId', 'upstream') >> { return 'goals upstream' }
        1 * getPipelineMock('util.getGoals')('projectB', 'propertiesFileId') >> { return 'goals current' }
        0 * getPipelineMock('util.getGoals')('projectC', 'propertiesFileId', _)
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goals upstream')
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goals current')
        0 * getPipelineMock('util.buildProject')('projectC', 'settingsXmlId', _)
        0 * getPipelineMock('maven.runMavenWithSettingsSonar')(_)
    }

    def "[pullrequest.groovy] build with sonarCloudReps without currentProject"() {
        when:
        groovyScript.build(projectCollection, 'projectB', 'settingsXmlId', 'propertiesFileId', 'sonarCloudId', ['projectA'])
        then:
        1 * getPipelineMock('util.checkoutProjects')(projectCollection, 'projectB')
        1 * getPipelineMock('util.getGoals')('projectA', 'propertiesFileId', 'upstream') >> { return 'goals upstream' }
        1 * getPipelineMock('util.getGoals')('projectB', 'propertiesFileId') >> { return 'goals current' }
        0 * getPipelineMock('util.getGoals')('projectC', 'propertiesFileId', _)
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goals upstream')
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goals current')
        0 * getPipelineMock('util.buildProject')('projectC', 'settingsXmlId', _)
        0 * getPipelineMock('maven.runMavenWithSettingsSonar')(_)
    }

    def "[pullrequest.groovy] build with sonarCloudReps with currentProject project triggering job"() {
        when:
        groovyScript.build(projectCollection, 'projectB', 'settingsXmlId', 'propertiesFileId', 'sonarCloudId', ['projectB'])
        then:
        1 * getPipelineMock('util.checkoutProjects')(projectCollection, 'projectB')
        1 * getPipelineMock('util.getGoals')('projectA', 'propertiesFileId', 'upstream') >> { return 'goals upstream' }
        1 * getPipelineMock('util.getGoals')('projectB', 'propertiesFileId') >> { return 'goals current' }
        0 * getPipelineMock('util.getGoals')('projectC', 'propertiesFileId', _)
        1 * getPipelineMock('util.getGoals')('projectB', 'propertiesFileId', 'sonarcloud') >> { return 'goals sonarcloud'}
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goals upstream')
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goals current')
        0 * getPipelineMock('util.buildProject')('projectC', 'settingsXmlId', _)
        1 * getPipelineMock('util.getProjectGroupName')('projectB') >> { return ['kiegroup', 'projectB']}
        1 * getPipelineMock('env.getProperty')('WORKSPACE') >> { return '/workspacefolder' }
        1 * getPipelineMock('dir')('/workspacefolder', _ as Closure)
        1 * getPipelineMock('maven.runMavenWithSettingsSonar')('settingsXmlId', 'goals sonarcloud', 'sonarCloudId', 'kiegroup_projectB.maven.log')
    }
}