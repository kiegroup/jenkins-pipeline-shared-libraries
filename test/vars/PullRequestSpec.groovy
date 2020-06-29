import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class PullRequestSpec extends JenkinsPipelineSpecification {
    def groovyScript = null
    def projectCollection = ['projectA', 'projectB', 'projectC']
    def projectCollectionLong = ['projectA', 'projectB', 'projectC', 'projectD', 'projectE']

    def setup() {
        groovyScript = loadPipelineScriptForTest("vars/pullrequest.groovy")
        explicitlyMockPipelineVariable("out")
        groovyScript.metaClass.WORKSPACE = '/'
    }

    def "[pullrequest.groovy] build without sonarCloudReps"() {
        when:
        groovyScript.build(projectCollection, 'projectB', 'settingsXmlId', 'propertiesFileId', 'sonarCloudId')
        then:
        1 * getPipelineMock('util.getProjectGroupName')('projectA') >> { return ['kiegroup', 'projectA']}
        0 * getPipelineMock('util.getProjectGroupName')('projectB')
        0 * getPipelineMock('util.getProjectGroupName')('projectC')

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
        1 * getPipelineMock('util.getProjectGroupName')('projectA') >> { return ['kiegroup', 'projectA']}
        0 * getPipelineMock('util.getProjectGroupName')('projectB')
        0 * getPipelineMock('util.getProjectGroupName')('projectC')

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
        1 * getPipelineMock('util.getProjectGroupName')('projectA') >> { return ['kiegroup', 'projectA']}
        0 * getPipelineMock('util.getProjectGroupName')('projectB')
        0 * getPipelineMock('util.getProjectGroupName')('projectC')

        1 * getPipelineMock('util.checkoutProjects')(projectCollection, 'projectB')
        1 * getPipelineMock('util.getGoals')('projectA', 'propertiesFileId', 'upstream') >> { return 'goals upstream' }
        1 * getPipelineMock('util.getGoals')('projectB', 'propertiesFileId') >> { return 'goals current' }
        0 * getPipelineMock('util.getGoals')('projectC', 'propertiesFileId', _)
        1 * getPipelineMock('util.getGoals')('projectB', 'propertiesFileId', 'sonarcloud') >> { return 'goals sonarcloud'}
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goals upstream')
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goals current')
        0 * getPipelineMock('util.buildProject')('projectC', 'settingsXmlId', _)
        1 * getPipelineMock('util.getProjectGroupName')('projectB') >> { return ['kiegroup', 'projectB']}
        1 * getPipelineMock('util.isProjectTriggeringJob')(['kiegroup', 'projectB']) >> { return true}
        2 * getPipelineMock('env.getProperty')('WORKSPACE') >> { return '/workspacefolder' }
        1 * getPipelineMock('dir')('/workspacefolder', _ as Closure)
        1 * getPipelineMock('maven.runMavenWithSettingsSonar')('settingsXmlId', 'goals sonarcloud', 'sonarCloudId', 'kiegroup_projectB.maven.log')
    }

    def "[pullrequest.groovy] build with sonarCloudReps without currentProject project triggering job"() {
        when:
        groovyScript.build(projectCollection, 'projectB', 'settingsXmlId', 'propertiesFileId', 'sonarCloudId', ['projectB'])
        then:
        1 * getPipelineMock('util.getProjectGroupName')('projectA') >> { return ['kiegroup', 'projectA']}
        0 * getPipelineMock('util.getProjectGroupName')('projectB')
        0 * getPipelineMock('util.getProjectGroupName')('projectC')

        1 * getPipelineMock('util.checkoutProjects')(projectCollection, 'projectB')
        1 * getPipelineMock('util.getGoals')('projectA', 'propertiesFileId', 'upstream') >> { return 'goals upstream' }
        1 * getPipelineMock('util.getGoals')('projectB', 'propertiesFileId') >> { return 'goals current' }
        0 * getPipelineMock('util.getGoals')('projectC', 'propertiesFileId', _)
        1 * getPipelineMock('util.getGoals')('projectB', 'propertiesFileId', 'sonarcloud') >> { return 'goals sonarcloud'}
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goals upstream')
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goals current')
        0 * getPipelineMock('util.buildProject')('projectC', 'settingsXmlId', _)
        1 * getPipelineMock('util.getProjectGroupName')('projectB') >> { return ['kiegroup', 'projectB']}
        1 * getPipelineMock('util.isProjectTriggeringJob')(['kiegroup', 'projectB']) >> { return false}
        2 * getPipelineMock('env.getProperty')('WORKSPACE') >> { return '/workspacefolder' }
        1 * getPipelineMock('dir')('/workspacefolder/kiegroup_projectB', _ as Closure)
        1 * getPipelineMock('maven.runMavenWithSettingsSonar')('settingsXmlId', 'goals sonarcloud', 'sonarCloudId', 'kiegroup_projectB.maven.log')
    }

    def "[pullrequest.groovy] build without long project collection"() {
        setup:
        def env = [:]
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'amazing_branch'
        env['WORKSPACE'] = 'workspacefolder'
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.build(projectCollectionLong, 'projectC', 'settingsXmlId', 'propertiesFileId', 'sonarCloudId')
        then:
        1 * getPipelineMock('util.getProjectGroupName')('projectA') >> { return ['kiegroup', 'projectA']}
        1 * getPipelineMock('util.getProjectGroupName')('projectB') >> { return ['kiegroup', 'projectB']}
        0 * getPipelineMock('util.getProjectGroupName')('projectC')
        0 * getPipelineMock('util.getProjectGroupName')('projectD')
        0 * getPipelineMock('util.getProjectGroupName')('projectE')

        1 * getPipelineMock('util.checkoutProjects')(projectCollectionLong, 'projectC')
        0 * getPipelineMock('util.getGoals')('projectA', _, _)
        1 * getPipelineMock('util.getGoals')('projectB', 'propertiesFileId', 'upstream') >> { return 'goals upstream' }
        1 * getPipelineMock('util.getGoals')('projectC', 'propertiesFileId') >> { return 'goals current' }
        0 * getPipelineMock('util.getGoals')('projectD', 'propertiesFileId', _)
        0 * getPipelineMock('util.getGoals')('projectE', 'propertiesFileId', _)
        0 * getPipelineMock('maven.runMavenWithSettingsSonar')(_)

        1 * getPipelineMock('maven.getPomArtifact')('workspacefolder/kiegroup_projectA/pom.xml') >> { return 'org.kie:project-a:1.0.0:pom'}
        1 * getPipelineMock('maven.artifactExists')('settingsXmlId', 'org.kie:project-a:1.0.0:pom') >> { return true}
        1 * getPipelineMock('githubscm.getRepositoryScm')('projectA', 'ginxo', 'amazing_branch') >> { return null}
        1 * getPipelineMock('maven.getPomArtifact')('workspacefolder/kiegroup_projectB/pom.xml') >> { return 'org.kie:project-b:1.0.0:pom'}
        1 * getPipelineMock('maven.artifactExists')('settingsXmlId', 'org.kie:project-b:1.0.0:pom') >> { return true}
        1 * getPipelineMock('githubscm.getRepositoryScm')('projectB', 'ginxo', 'amazing_branch') >> { return 'notnull'}

        0 * getPipelineMock('util.buildProject')('projectA', _, _, _)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goals upstream')
        1 * getPipelineMock('util.buildProject')('projectC', 'settingsXmlId', 'goals current')
        0 * getPipelineMock('util.buildProject')('projectD', 'settingsXmlId', _)
        0 * getPipelineMock('util.buildProject')('projectE', 'settingsXmlId', _)
    }

//    def "[pullrequest.groovy] upstreamBuild with collection with already existing projects"() {
//        setup:
//        def env = [:]
//        env['CHANGE_AUTHOR'] = 'ginxo'
//        env['CHANGE_BRANCH'] = 'amazing_branch'
//        env['WORKSPACE'] = 'workspacefolder'
//        groovyScript.getBinding().setVariable("env", env)
//        when:
//        groovyScript.upstreamBuild(projectCollection, 'projectB', 'settingsXmlId', 'goalX')
//        then:
//        1 * getPipelineMock('util.getProjectTriggeringJob')() >> ['kiegroup', 'projectB']
//        1 * getPipelineMock('util.getProjectGroupName')('projectA') >> { return ['kiegroup', 'projectA']}
//        1 * getPipelineMock('maven.getPomArtifact')('workspacefolder/kiegroup_projectA/pom.xml') >> { return 'org.kie:project-a:1.0.0:pom'}
//        1 * getPipelineMock('maven.artifactExists')('settingsXmlId', 'org.kie:project-a:1.0.0:pom') >> { return true}
//        1 * getPipelineMock('githubscm.getRepositoryScm')('projectA', 'ginxo', 'amazing_branch') >> { return null}
//
//        1 * getPipelineMock('util.checkoutProjects')([['projectA', 'projectB', 'projectC'], 'projectB'])
//        0 * getPipelineMock('util.buildProject')('projectA', _, _, _)
//        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalX', null)
//    }

    def "[pullrequest.groovy] hasProjectChangingBranch exist"() {
        setup:
        def env = [:]
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'amazing_branch'
        groovyScript.getBinding().setVariable("env", env)
        when:
        def result = groovyScript.hasProjectChangingBranch('projectA')
        then:
        1 * getPipelineMock('githubscm.getRepositoryScm')('projectA', 'ginxo', 'amazing_branch') >> { return 'notnull'}
        result
    }

    def "[pullrequest.groovy] hasProjectChangingBranch not exist"() {
        setup:
        def env = [:]
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'amazing_branch'
        groovyScript.getBinding().setVariable("env", env)
        when:
        def result = groovyScript.hasProjectChangingBranch('projectA')
        then:
        1 * getPipelineMock('githubscm.getRepositoryScm')('projectA', 'ginxo', 'amazing_branch') >> { return null}
        !result
    }

    def "[pullrequest.groovy] getAlreadyExistingUpperLevelProjects"() {
        setup:
        def env = [:]
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'amazing_branch'
        env['WORKSPACE'] = 'workspacefolder'
        groovyScript.getBinding().setVariable("env", env)
        when:
        List<String> result = groovyScript.getAlreadyExistingUpperLevelProjects(projectCollectionLong, 'projectD', 'settingsXmlId')
        then:
        1 * getPipelineMock('util.getProjectGroupName')('projectA') >> { return ['kiegroup', 'projectA']}
        1 * getPipelineMock('util.getProjectGroupName')('projectB') >> { return ['kiegroup', 'projectB']}
        1 * getPipelineMock('util.getProjectGroupName')('projectC') >> { return ['kiegroup', 'projectC']}
        0 * getPipelineMock('util.getProjectGroupName')('projectD')
        0 * getPipelineMock('util.getProjectGroupName')('projectE')

        1 * getPipelineMock('maven.getPomArtifact')('workspacefolder/kiegroup_projectA/pom.xml') >> { return 'org.kie:project-a:1.0.0:pom'}
        1 * getPipelineMock('maven.getPomArtifact')('workspacefolder/kiegroup_projectB/pom.xml') >> { return 'org.kie:project-b:1.0.0:pom'}
        1 * getPipelineMock('maven.getPomArtifact')('workspacefolder/kiegroup_projectC/pom.xml') >> { return 'org.kie:project-c:1.0.0:pom'}
        0 * getPipelineMock('maven.getPomArtifact')('workspacefolder/kiegroup_projectD/pom.xml')
        0 * getPipelineMock('maven.getPomArtifact')('workspacefolder/kiegroup_projectE/pom.xml')
        1 * getPipelineMock('maven.artifactExists')('settingsXmlId', 'org.kie:project-a:1.0.0:pom') >> { return true}
        1 * getPipelineMock('maven.artifactExists')('settingsXmlId', 'org.kie:project-b:1.0.0:pom') >> { return true}
        1 * getPipelineMock('maven.artifactExists')('settingsXmlId', 'org.kie:project-c:1.0.0:pom') >> { return false}


        1 * getPipelineMock('githubscm.getRepositoryScm')('projectA', 'ginxo', 'amazing_branch') >> { return 'notnull'}
        1 * getPipelineMock('githubscm.getRepositoryScm')('projectB', 'ginxo', 'amazing_branch') >> { return null}
        0 * getPipelineMock('githubscm.getRepositoryScm')('projectC', 'ginxo', 'amazing_branch')
        0 * getPipelineMock('githubscm.getRepositoryScm')('projectD', 'ginxo', 'amazing_branch')
        0 * getPipelineMock('githubscm.getRepositoryScm')('projectE', 'ginxo', 'amazing_branch')
        result.size() == 1
        result[0] == 'projectB'
    }
}