import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import hudson.plugins.git.GitSCM

class UtilSpec extends JenkinsPipelineSpecification {
    def groovyScript = null
    def projectBranchMappingProperties = null


    def setup() {
        groovyScript = loadPipelineScriptForTest("vars/util.groovy")
        explicitlyMockPipelineVariable("out")

        groovyScript.getBinding().setVariable('PROPERTIES_FILE', 'project-branches-mapping.properties')
        projectBranchMappingProperties = new Properties()
        this.getClass().getResource( '/project-branches-mapping.properties' ).withInputStream {
            projectBranchMappingProperties.load(it)
        }
    }

    def "[util.groovy] checkoutProject without mapping and triggering job null"() {
        setup:
        def env = [:]
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = 'master'
        env ['ghprbGhRepository'] = 'projectB'
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.checkoutProject('projectA', 'kiegroup')
        then:
        1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
        1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
            return projectBranchMappingProperties
        }
        1 * getPipelineMock("githubscm.checkoutIfExists")('projectA', 'ginxo', 'branch1', 'kiegroup', 'master', true)

        1 * getPipelineMock('githubscm.getCommit')() >> 'kiegroup/lienzo-core: 0f917d4 Expose zoom and pan filters (#102)'
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/master'
        1 * getPipelineMock('githubscm.getRemoteInfo')('origin', 'url') >> 'https://github.com/kiegroup/lienzo-core.git'
    }

    def "[util.groovy] checkoutProject without mapping and not triggering job"() {
        setup:
        def env = [:]
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = 'master'
        env ['ghprbGhRepository'] = 'projectB'
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.checkoutProject('projectA', 'kiegroup', false)
        then:
        1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
        1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
            return projectBranchMappingProperties
        }
        1 * getPipelineMock("githubscm.checkoutIfExists")('projectA', 'ginxo', 'branch1', 'kiegroup', 'master', true)

        1 * getPipelineMock('githubscm.getCommit')() >> 'kiegroup/lienzo-core: 0f917d4 Expose zoom and pan filters (#102)'
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/master'
        1 * getPipelineMock('githubscm.getRemoteInfo')('origin', 'url') >> 'https://github.com/kiegroup/lienzo-core.git'
    }

    def "[util.groovy] checkoutProject with mapping and not triggering job"() {
        setup:
        def env = [:]
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = '7.x'
        env ['ghprbGhRepository'] = 'optaplanner'
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.checkoutProject('projectA', 'kiegroup', false)
        then:
        1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
        1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
            return projectBranchMappingProperties
        }
        1 * getPipelineMock("githubscm.checkoutIfExists")('projectA', 'ginxo', 'branch1', 'kiegroup', 'master', true)

        1 * getPipelineMock('githubscm.getCommit')() >> 'kiegroup/lienzo-core: 0f917d4 Expose zoom and pan filters (#102)'
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/master'
        1 * getPipelineMock('githubscm.getRemoteInfo')('origin', 'url') >> 'https://github.com/kiegroup/lienzo-core.git'
    }

    def "[util.groovy] checkoutProject with mapping and triggering job"() {
        setup:
        def env = [:]
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = '7.x'
        env ['ghprbGhRepository'] = 'optaplanner'
        groovyScript.getBinding().setVariable("env", env)
        groovyScript.getBinding().setVariable("CHANGE_FORK", 'ginxo1')
        when:
        groovyScript.checkoutProject('optaplanner', 'kiegroup', true)
        then:
        1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
        1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
            return projectBranchMappingProperties
        }
        1 * getPipelineMock("githubscm.mergeSourceIntoTarget")('optaplanner', 'ginxo1', 'branch1', 'kiegroup', '7.x')

        1 * getPipelineMock('githubscm.getCommit')() >> 'kiegroup/lienzo-core: 0f917d4 Expose zoom and pan filters (#102)'
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/master'
        1 * getPipelineMock('githubscm.getRemoteInfo')('origin', 'url') >> 'https://github.com/kiegroup/lienzo-core.git'
    }

    def "[util.groovy] getProject"() {
        when:
        def project = groovyScript.getProject('https://github.com/kiegroup/jenkins-pipeline-shared-libraries.git')
        then:
        project == 'kiegroup/jenkins-pipeline-shared-libraries'
    }

    def "[util.groovy] getGroup"() {
        when:
        def group = groovyScript.getGroup('https://github.com/kiegroup/jenkins-pipeline-shared-libraries.git')
        then:
        group == 'kiegroup'
    }

    def "[util.groovy] getProjectGroupName with group"() {
        when:
        def groupName = groovyScript.getProjectGroupName('name', 'group')
        then:
        groupName[0] == 'group'
        groupName[1] == 'name'
    }

    def "[util.groovy] getProjectGroupName without group"() {
        when:
        def groupName = groovyScript.getProjectGroupName('name')
        then:
        groupName[0] == 'kiegroup'
        groupName[1] == 'name'
    }

    def "[util.groovy] getGoals with type"() {
        setup:
        groovyScript.getBinding().setVariable('PROPERTIES_FILE', 'propertiesFile.txt')
        def properties = new Properties()
        this.getClass().getResource( '/goals.properties' ).withInputStream {
            properties.load(it)
        }
        when:
        def goals = groovyScript.getGoals('project1', 'fileId', 'typex')
        then:
        1 * getPipelineMock("configFile.call")(['fileId': 'fileId', 'variable': 'PROPERTIES_FILE']) >> { return 'configFile' }
        1 * getPipelineMock("configFileProvider.call")(['configFile'], _ as Closure) >> properties."goals.project1.typex"
        1 * getPipelineMock("readProperties")(['file': 'propertiesFile.txt']) >> {
            return properties
        }
        goals == "typexValue"
    }

    def "[util.groovy] isProjectTriggeringJob true"() {
        setup:
        def projectGroupName = ['group', 'name']
        def env = [:]
        env['ghprbGhRepository'] = 'group/name'
        groovyScript.getBinding().setVariable("env", env)
        when:
        def result = groovyScript.isProjectTriggeringJob(projectGroupName)
        then:
        result
    }

    def "[util.groovy] isProjectTriggeringJob false different project"() {
        setup:
        def projectGroupName = ['group', 'name']
        def env = [:]
        env['ghprbGhRepository'] = 'group/name1'
        groovyScript.getBinding().setVariable("env", env)
        when:
        def result = groovyScript.isProjectTriggeringJob(projectGroupName)
        then:
        !result
    }

    def "[util.groovy] storeGitInformation GIT_INFORMATION_REPORT null"() {
        setup:
        def projectGroupName = ['group', 'name']
        def env = [:]
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.storeGitInformation('projectName')
        then:
        1 * getPipelineMock('githubscm.getCommit')() >> 'kiegroup/lienzo-core: 0f917d4 Expose zoom and pan filters (#102)'
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/master'
        1 * getPipelineMock('githubscm.getRemoteInfo')('origin', 'url') >> 'https://github.com/kiegroup/lienzo-core.git'
        env['GIT_INFORMATION_REPORT'] == "projectName=kiegroup/lienzo-core: 0f917d4 Expose zoom and pan filters (#102) Branch [* (detached from 0f917d4)  remotes/origin/master] Remote [https://github.com/kiegroup/lienzo-core.git]"
    }

    def "[util.groovy] storeGitInformation GIT_INFORMATION_REPORT previous value"() {
        setup:
        def projectGroupName = ['group', 'name']
        def env = [:]
        env['GIT_INFORMATION_REPORT'] = 'projectName=kiegroup/lienzo-tests: 45c16e1 Fix tests (#84) Branch [* (detached from 45c16e1)  remotes/origin/master] Remote [https://github.com/kiegroup/lienzo-tests.git]'
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.storeGitInformation('projectName')
        then:
        1 * getPipelineMock('githubscm.getCommit')() >> 'kiegroup/lienzo-core: 0f917d4 Expose zoom and pan filters (#102)'
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/master'
        1 * getPipelineMock('githubscm.getRemoteInfo')('origin', 'url') >> 'https://github.com/kiegroup/lienzo-core.git'
        env['GIT_INFORMATION_REPORT'] == 'projectName=kiegroup/lienzo-tests: 45c16e1 Fix tests (#84) Branch [* (detached from 45c16e1)  remotes/origin/master] Remote [https://github.com/kiegroup/lienzo-tests.git]; projectName=kiegroup/lienzo-core: 0f917d4 Expose zoom and pan filters (#102) Branch [* (detached from 0f917d4)  remotes/origin/master] Remote [https://github.com/kiegroup/lienzo-core.git]'
    }

    def "[util.groovy] printGitInformationReport GIT_INFORMATION_REPORT null"() {
        setup:
        def projectGroupName = ['group', 'name']
        def env = [:]
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.printGitInformationReport()
        then:
        true
    }
}