import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class TreeBuildSpec extends JenkinsPipelineSpecification {
    def groovyScript = null
    def projectMap = ['projectA': ['goalA'], 'projectB': ['goalB1', 'goalB2'], 'projectC': ['goalC']]
    def projectMapLong = ['projectA': ['goalA'], 'projectB': ['goalB1', 'goalB2'], 'projectC': ['goalC'], 'projectD': ['goalD'], 'projectE': ['goalE']]
    def projectCollection = ['projectA', 'projectB', 'projectC']

    def setup() {
        groovyScript = loadPipelineScriptForTest("vars/treebuild.groovy")
        explicitlyMockPipelineVariable("out")
    }

    def "[treebuild.groovy] build with map and skipTests"() {
        when:
        groovyScript.build(projectMap, 'settingsXmlId', true)
        then:
        1 * getPipelineMock('util.getProjectTriggeringJob')() >> ['kiegroup', 'projectA']

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
        1 * getPipelineMock('util.getProjectTriggeringJob')() >> ['kiegroup', 'projectA']

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
        1 * getPipelineMock('util.getProjectTriggeringJob')() >> ['kiegroup', 'projectA']

        1 * getPipelineMock('util.checkoutProjects')([['projectA', 'projectB', 'projectC'], 'projectC'])
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goalX', false)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalX', false)
        1 * getPipelineMock('util.buildProject')('projectC', 'settingsXmlId', 'goalX', false)
    }

    def "[treebuild.groovy] build with collection without skipTests"() {
        when:
        groovyScript.build(projectCollection, 'settingsXmlId', 'goalX')
        then:
        1 * getPipelineMock('util.getProjectTriggeringJob')() >> ['kiegroup', 'projectA']

        1 * getPipelineMock('util.checkoutProjects')([['projectA', 'projectB', 'projectC'], 'projectC'])
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goalX', null)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalX', null)
        1 * getPipelineMock('util.buildProject')('projectC', 'settingsXmlId', 'goalX', null)
    }

    def "[treebuild.groovy] upstreamBuild with map and skipTests"() {
        when:
        groovyScript.upstreamBuild(projectMap, 'projectB', 'settingsXmlId', true)
        then:
        1 * getPipelineMock('util.getProjectTriggeringJob')() >> ['kiegroup', 'projectA']

        1 * getPipelineMock('util.checkoutProjects')(['projectA', 'projectB', 'projectC'], 'projectB')
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goalA', true)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalB1', true)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalB2', true)
    }

    def "[treebuild.groovy] upstreamBuild with map without skipTests"() {
        when:
        groovyScript.upstreamBuild(projectMap, 'projectB', 'settingsXmlId')
        then:
        1 * getPipelineMock('util.getProjectTriggeringJob')() >> ['kiegroup', 'projectA']

        1 * getPipelineMock('util.checkoutProjects')(['projectA', 'projectB', 'projectC'], 'projectB')
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goalA', null)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalB1', null)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalB2', null)
    }

    def "[treebuild.groovy] upstreamBuild with collection with skipTests"() {
        when:
        groovyScript.upstreamBuild(projectCollection, 'projectB', 'settingsXmlId', 'goalX', false)
        then:
        1 * getPipelineMock('util.getProjectTriggeringJob')() >> ['kiegroup', 'projectA']

        1 * getPipelineMock('util.checkoutProjects')([['projectA', 'projectB', 'projectC'], 'projectB'])
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goalX', false)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalX', false)
    }

    def "[treebuild.groovy] upstreamBuild with collection without skipTests"() {
        when:
        groovyScript.upstreamBuild(projectCollection, 'projectB', 'settingsXmlId', 'goalX')
        then:
        1 * getPipelineMock('util.getProjectTriggeringJob')() >> ['kiegroup', 'projectA']

        1 * getPipelineMock('util.checkoutProjects')([['projectA', 'projectB', 'projectC'], 'projectB'])
        1 * getPipelineMock('util.buildProject')('projectA', 'settingsXmlId', 'goalX', null)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalX', null)
    }

    def "[treebuild.groovy] upstreamBuild with collection with already existing projects"() {
        setup:
        def env = [:]
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'amazing_branch'
        env['WORKSPACE'] = 'workspacefolder'
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.upstreamBuild(projectCollection, 'projectB', 'settingsXmlId', 'goalX')
        then:
        1 * getPipelineMock('util.getProjectTriggeringJob')() >> ['kiegroup', 'projectB']
        1 * getPipelineMock('util.getProjectGroupName')('projectA') >> { return ['kiegroup', 'projectA']}
        1 * getPipelineMock('maven.getPomArtifact')('workspacefolder/kiegroup_projectA/pom.xml') >> { return 'org.kie:project-a:1.0.0:pom'}
        1 * getPipelineMock('maven.artifactExists')('settingsXmlId', 'org.kie:project-a:1.0.0:pom') >> { return true}
        1 * getPipelineMock('githubscm.getRepositoryScm')('projectA', 'ginxo', 'amazing_branch') >> { return null}

        1 * getPipelineMock('util.checkoutProjects')([['projectA', 'projectB', 'projectC'], 'projectB'])
        0 * getPipelineMock('util.buildProject')('projectA', _, _, _)
        1 * getPipelineMock('util.buildProject')('projectB', 'settingsXmlId', 'goalX', null)
    }

    def "[treebuild.groovy] hasProjectChangingBranch exist"() {
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

    def "[treebuild.groovy] hasProjectChangingBranch not exist"() {
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

    def "[treebuild.groovy] getAlreadyExistingUpperLevelProjects"() {
        setup:
        def env = [:]
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'amazing_branch'
        env['WORKSPACE'] = 'workspacefolder'
        groovyScript.getBinding().setVariable("env", env)
        when:
        List<String> result = groovyScript.getAlreadyExistingUpperLevelProjects(projectMapLong, 'settingsXmlId')
        then:
        1 * getPipelineMock('util.getProjectTriggeringJob')() >> { return ['kiegroup', 'projectD']}
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