import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class PmeBuildSpec extends JenkinsPipelineSpecification {
    def groovyScript = null
    def projectCollection = ['projectA', 'projectB', 'projectC', 'projectD']
    def buildConfigContent

    def setup() {
        groovyScript = loadPipelineScriptForTest("vars/pmebuild.groovy")
        explicitlyMockPipelineVariable("out")
    }

    def "[pmebuild.groovy] build with branch and change author"() {
        setup:
        def env = [:]
        env['PRODUCT_VERSION'] = '1.0.0'
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'amazing_branch'
        env['WORKSPACE'] = '/workspacefolder'
        groovyScript.getBinding().setVariable("env", env)

        def url = getClass().getResource('/build-config.yaml')
        this.buildConfigContent = new File(url.toURI()).text

        groovyScript.metaClass.PME_MAVEN_SETTINGS_XML = 'pmeMavenSettingsFileId'
        groovyScript.metaClass.SKIP_TESTS = 'true'


        when:
        groovyScript.buildProjects(projectCollection, 'settingsXmlId', 'buildConfigPathFolder', 'pmeCliPath', ['variable1': 'value1'], ['projectB-scmRevision': 'branchX', 'projectD-scmRevision': 'branchDX'], ['version1': 'valueVersion1'])
        then:
        1 * getPipelineMock('readFile')('buildConfigPathFolder/build-config.yaml') >> { return this.buildConfigContent}
        1 * getPipelineMock('util.getProjectGroupName')('projectA', 'kiegroup') >> { return ['kiegroup', 'projectA']}
        1 * getPipelineMock('util.getProjectGroupName')('projectB', 'kiegroup') >> { return ['kiegroup', 'projectB']}
        1 * getPipelineMock('util.getProjectGroupName')('projectC', 'kiegroup') >> { return ['kiegroup', 'projectC']}
        1 * getPipelineMock('util.getProjectGroupName')('projectD', 'kiegroup') >> { return ['kiegroup', 'projectD']}
        1 * getPipelineMock('util.getProjectGroupName')('projectA') >> { return ['kiegroup', 'projectA']}
        1 * getPipelineMock('util.getProjectGroupName')('projectB') >> { return ['kiegroup', 'projectB']}
        1 * getPipelineMock('util.getProjectGroupName')('projectC') >> { return ['kiegroup', 'projectC']}
        1 * getPipelineMock('util.getProjectGroupName')('projectD') >> { return ['kiegroup', 'projectD']}

        1 * getPipelineMock('fileExists')('/workspacefolder/kiegroup_projectA') >> false
        1 * getPipelineMock('fileExists')('/workspacefolder/kiegroup_projectB') >> false
        1 * getPipelineMock('fileExists')('/workspacefolder/kiegroup_projectC') >> true
        1 * getPipelineMock('fileExists')('/workspacefolder/kiegroup_projectD') >> false

        2 * getPipelineMock('dir')('/workspacefolder/kiegroup_projectA', _ as Closure)
        2 * getPipelineMock('dir')('/workspacefolder/kiegroup_projectB', _ as Closure)
        1 * getPipelineMock('dir')('/workspacefolder/kiegroup_projectC', _ as Closure)
        2 * getPipelineMock('dir')('/workspacefolder/kiegroup_projectD', _ as Closure)

        1 * getPipelineMock('githubscm.checkoutIfExists')('projectA', 'ginxo', 'amazing_branch', 'kiegroup', 'amazing_branch')
        1 * getPipelineMock('githubscm.checkoutIfExists')('projectB', 'ginxo', 'branchX', 'kiegroup', 'branchX')
        0 * getPipelineMock('githubscm.checkoutIfExists')('projectC', _, _, _, _)
        1 * getPipelineMock('githubscm.checkoutIfExists')('projectD', 'ginxo', 'branchDX', 'kiegroup', 'branchDX')

        1 * getPipelineMock('maven.runMavenWithSettings')('settingsXmlId', 'deploy -B -Dfull=true -Drevapi.skip=true -Denforcer.skip=true -Dgwt.compiler.localWorkers=1 -Dproductized=true -Dfindbugs.skip=true -Dcheckstyle.skip=true -DaltDeploymentRepository=local::default::file:///workspacefolder/deployDirectory', true, 'kiegroup_projectA.maven.log')
        1 * getPipelineMock('maven.runMavenWithSettings')('settingsXmlId', 'deploy -B -Dfull=true -Drevapi.skip=true -Denforcer.skip=true -Dgwt.compiler.localWorkers=1 -Dproductized=true -Dfindbugs.skip=true -Dcheckstyle.skip=true -DaltDeploymentRepository=local::default::file:///workspacefolder/deployDirectory', true, 'kiegroup_projectB.maven.log')
        1 * getPipelineMock('maven.runMavenWithSettings')('settingsXmlId', 'deploy -B -Dfull=true -Drevapi.skip=true -Denforcer.skip=true -Dgwt.compiler.localWorkers=1 -Dproductized=true -Dfindbugs.skip=true -Dcheckstyle.skip=true -DaltDeploymentRepository=local::default::file:///workspacefolder/deployDirectory', true, 'kiegroup_projectC.maven.log')
        1 * getPipelineMock('maven.runMavenWithSettings')('settingsXmlId', 'deploy -B -Dfull=true -Drevapi.skip=true -Denforcer.skip=true -Dgwt.compiler.localWorkers=1 -Dproductized=true -Dfindbugs.skip=true -Dcheckstyle.skip=true -DaltDeploymentRepository=local::default::file:///workspacefolder/deployDirectory', true, 'kiegroup_projectD.maven.log')
        assert env['PME_BUILD_VARIABLES'].contains('projectB-scmRevision=branchX')
        assert env['PME_BUILD_VARIABLES'].contains('projectC-scmRevision={{scmRevision}}')
        assert env['PME_BUILD_VARIABLES'].contains('projectD-scmRevision=branchDX')
    }



    def "[pmebuild.groovy] build with branch and change author without additional variables"() {
        setup:
        def env = [:]
        env['PRODUCT_VERSION'] = '1.0.0'
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'amazing_branch'
        env['WORKSPACE'] = '/workspacefolder'
        groovyScript.getBinding().setVariable("env", env)

        def url = getClass().getResource('/build-config.yaml')
        this.buildConfigContent = new File(url.toURI()).text

        groovyScript.metaClass.PME_MAVEN_SETTINGS_XML = 'pmeMavenSettingsFileId'
        groovyScript.metaClass.SKIP_TESTS = 'true'


        when:
        groovyScript.buildProjects(projectCollection, 'settingsXmlId', 'buildConfigPathFolder', 'pmeCliPath', ['variable1': 'value1'], ['version1': 'valueVersion1'])
        then:
        1 * getPipelineMock('readFile')('buildConfigPathFolder/build-config.yaml') >> { return this.buildConfigContent}
        1 * getPipelineMock('util.getProjectGroupName')('projectA', 'kiegroup') >> { return ['kiegroup', 'projectA']}
        1 * getPipelineMock('util.getProjectGroupName')('projectB', 'kiegroup') >> { return ['kiegroup', 'projectB']}
        1 * getPipelineMock('util.getProjectGroupName')('projectC', 'kiegroup') >> { return ['kiegroup', 'projectC']}
        1 * getPipelineMock('util.getProjectGroupName')('projectD', 'kiegroup') >> { return ['kiegroup', 'projectD']}
        1 * getPipelineMock('util.getProjectGroupName')('projectA') >> { return ['kiegroup', 'projectA']}
        1 * getPipelineMock('util.getProjectGroupName')('projectB') >> { return ['kiegroup', 'projectB']}
        1 * getPipelineMock('util.getProjectGroupName')('projectC') >> { return ['kiegroup', 'projectC']}
        1 * getPipelineMock('util.getProjectGroupName')('projectD') >> { return ['kiegroup', 'projectD']}

        1 * getPipelineMock('fileExists')('/workspacefolder/kiegroup_projectA') >> false
        1 * getPipelineMock('fileExists')('/workspacefolder/kiegroup_projectB') >> false
        1 * getPipelineMock('fileExists')('/workspacefolder/kiegroup_projectC') >> true
        1 * getPipelineMock('fileExists')('/workspacefolder/kiegroup_projectD') >> false

        2 * getPipelineMock('dir')('/workspacefolder/kiegroup_projectA', _ as Closure)
        2 * getPipelineMock('dir')('/workspacefolder/kiegroup_projectB', _ as Closure)
        1 * getPipelineMock('dir')('/workspacefolder/kiegroup_projectC', _ as Closure)
        2 * getPipelineMock('dir')('/workspacefolder/kiegroup_projectD', _ as Closure)

        1 * getPipelineMock('githubscm.checkoutIfExists')('projectA', 'ginxo', 'amazing_branch', 'kiegroup', 'amazing_branch')
        1 * getPipelineMock('githubscm.checkoutIfExists')('projectB', 'ginxo', 'amazing_branch', 'kiegroup', 'master')
        0 * getPipelineMock('githubscm.checkoutIfExists')('projectC', _, _, _, _)
        1 * getPipelineMock('githubscm.checkoutIfExists')('projectD', 'ginxo', 'amazing_branch', 'kiegroup', 'amazing_branch')

        1 * getPipelineMock('maven.runMavenWithSettings')('settingsXmlId', 'deploy -B -Dfull=true -Drevapi.skip=true -Denforcer.skip=true -Dgwt.compiler.localWorkers=1 -Dproductized=true -Dfindbugs.skip=true -Dcheckstyle.skip=true -DaltDeploymentRepository=local::default::file:///workspacefolder/deployDirectory', true, 'kiegroup_projectA.maven.log')
        1 * getPipelineMock('maven.runMavenWithSettings')('settingsXmlId', 'deploy -B -Dfull=true -Drevapi.skip=true -Denforcer.skip=true -Dgwt.compiler.localWorkers=1 -Dproductized=true -Dfindbugs.skip=true -Dcheckstyle.skip=true -DaltDeploymentRepository=local::default::file:///workspacefolder/deployDirectory', true, 'kiegroup_projectB.maven.log')
        1 * getPipelineMock('maven.runMavenWithSettings')('settingsXmlId', 'deploy -B -Dfull=true -Drevapi.skip=true -Denforcer.skip=true -Dgwt.compiler.localWorkers=1 -Dproductized=true -Dfindbugs.skip=true -Dcheckstyle.skip=true -DaltDeploymentRepository=local::default::file:///workspacefolder/deployDirectory', true, 'kiegroup_projectC.maven.log')
        1 * getPipelineMock('maven.runMavenWithSettings')('settingsXmlId', 'deploy -B -Dfull=true -Drevapi.skip=true -Denforcer.skip=true -Dgwt.compiler.localWorkers=1 -Dproductized=true -Dfindbugs.skip=true -Dcheckstyle.skip=true -DaltDeploymentRepository=local::default::file:///workspacefolder/deployDirectory', true, 'kiegroup_projectD.maven.log')
        assert env['PME_BUILD_VARIABLES'].contains('projectB-scmRevision={{scmRevision}}')
        assert env['PME_BUILD_VARIABLES'].contains('projectC-scmRevision={{scmRevision}}')
        assert !env['PME_BUILD_VARIABLES'].contains('projectD-scmRevision')
    }
}
