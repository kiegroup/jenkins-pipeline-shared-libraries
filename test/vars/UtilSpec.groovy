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
        this.getClass().getResource('/project-branches-mapping.properties').withInputStream {
            projectBranchMappingProperties.load(it)
        }
    }

    def "[util.groovy] checkout. Trigger: Optaplanner, target: master, checkout project: optaplanner"() {
        setup:
        def trigger = 'optaplanner'
        def target = 'master'
        def mapping = 'master'
        def checkoutProject = trigger

        def env = [:]
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = target
        env['ghprbGhRepository'] = trigger
        groovyScript.getBinding().setVariable("CHANGE_FORK", 'ginxo')

        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.checkoutProject(checkoutProject, 'kiegroup')
        then:
        if (trigger == checkoutProject) {
            1 * getPipelineMock("githubscm.mergeSourceIntoTarget")(checkoutProject, 'ginxo', 'branch1', 'kiegroup', mapping)
        } else {
            1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
            1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
                return projectBranchMappingProperties
            }
            1 * getPipelineMock("githubscm.checkoutIfExists")(checkoutProject, 'ginxo', 'branch1', 'kiegroup', mapping, true)
        }

        1 * getPipelineMock('githubscm.getCommit')() >> 'kiegroup/lienzo-core: 0f917d4 Expose zoom and pan filters (#102)'
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/master'
        1 * getPipelineMock('githubscm.getRemoteInfo')('origin', 'url') >> 'https://github.com/kiegroup/lienzo-core.git'
    }

    def "[util.groovy] checkout. Trigger: Optaplanner, target: 7.x, checkout project: optaplanner"() {
        setup:
        def trigger = 'optaplanner'
        def target = '7.x'
        def mapping = '7.x'
        def checkoutProject = trigger

        def env = [:]
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = target
        env['ghprbGhRepository'] = trigger
        groovyScript.getBinding().setVariable("CHANGE_FORK", 'ginxo')

        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.checkoutProject(checkoutProject, 'kiegroup')
        then:
        if (trigger == checkoutProject) {
            1 * getPipelineMock("githubscm.mergeSourceIntoTarget")(checkoutProject, 'ginxo', 'branch1', 'kiegroup', mapping)
        } else {
            1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
            1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
                return projectBranchMappingProperties
            }
            1 * getPipelineMock("githubscm.checkoutIfExists")(checkoutProject, 'ginxo', 'branch1', 'kiegroup', mapping, true)
        }

        1 * getPipelineMock('githubscm.getCommit')() >> 'kiegroup/lienzo-core: 0f917d4 Expose zoom and pan filters (#102)'
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/master'
        1 * getPipelineMock('githubscm.getRemoteInfo')('origin', 'url') >> 'https://github.com/kiegroup/lienzo-core.git'
    }

    def "[util.groovy] checkout. Trigger: Optaplanner, target: master, checkout project: projectA"() {
        setup:
        def trigger = 'optaplanner'
        def target = 'master'
        def mapping = 'master'
        def checkoutProject = 'porjectA'

        def env = [:]
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = target
        env['ghprbGhRepository'] = trigger
        groovyScript.getBinding().setVariable("CHANGE_FORK", 'ginxo')

        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.checkoutProject(checkoutProject, 'kiegroup')
        then:
        if (trigger == checkoutProject) {
            1 * getPipelineMock("githubscm.mergeSourceIntoTarget")(checkoutProject, 'ginxo', 'branch1', 'kiegroup', mapping)
        } else {
            1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
            1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
                return projectBranchMappingProperties
            }
            1 * getPipelineMock("githubscm.checkoutIfExists")(checkoutProject, 'ginxo', 'branch1', 'kiegroup', mapping, true)
        }

        1 * getPipelineMock('githubscm.getCommit')() >> 'kiegroup/lienzo-core: 0f917d4 Expose zoom and pan filters (#102)'
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/master'
        1 * getPipelineMock('githubscm.getRemoteInfo')('origin', 'url') >> 'https://github.com/kiegroup/lienzo-core.git'
    }

    def "[util.groovy] checkout. Trigger: Optaplanner, target: 7.x, checkout project: projectA"() {
        setup:
        def trigger = 'optaplanner'
        def target = '7.x'
        def mapping = 'master'
        def checkoutProject = 'projectA'

        def env = [:]
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = target
        env['ghprbGhRepository'] = trigger
        groovyScript.getBinding().setVariable("CHANGE_FORK", 'ginxo')

        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.checkoutProject(checkoutProject, 'kiegroup')
        then:
        if (trigger == checkoutProject) {
            1 * getPipelineMock("githubscm.mergeSourceIntoTarget")(checkoutProject, 'ginxo', 'branch1', 'kiegroup', mapping)
        } else {
            1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
            1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
                return projectBranchMappingProperties
            }
            1 * getPipelineMock("githubscm.checkoutIfExists")(checkoutProject, 'ginxo', 'branch1', 'kiegroup', mapping, true)
        }

        1 * getPipelineMock('githubscm.getCommit')() >> 'kiegroup/lienzo-core: 0f917d4 Expose zoom and pan filters (#102)'
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/master'
        1 * getPipelineMock('githubscm.getRemoteInfo')('origin', 'url') >> 'https://github.com/kiegroup/lienzo-core.git'
    }

    def "[util.groovy] checkout. Trigger: projectA, target: master, checkout project: projectA"() {
        setup:
        def trigger = 'projectA'
        def target = 'master'
        def mapping = 'master'
        def checkoutProject = trigger

        def env = [:]
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = target
        env['ghprbGhRepository'] = trigger
        groovyScript.getBinding().setVariable("CHANGE_FORK", 'ginxo')

        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.checkoutProject(checkoutProject, 'kiegroup')
        then:
        if (trigger == checkoutProject) {
            1 * getPipelineMock("githubscm.mergeSourceIntoTarget")(checkoutProject, 'ginxo', 'branch1', 'kiegroup', mapping)
        } else {
            1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
            1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
                return projectBranchMappingProperties
            }
            1 * getPipelineMock("githubscm.checkoutIfExists")(checkoutProject, 'ginxo', 'branch1', 'kiegroup', mapping, true)
        }

        1 * getPipelineMock('githubscm.getCommit')() >> 'kiegroup/lienzo-core: 0f917d4 Expose zoom and pan filters (#102)'
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/master'
        1 * getPipelineMock('githubscm.getRemoteInfo')('origin', 'url') >> 'https://github.com/kiegroup/lienzo-core.git'
    }

    def "[util.groovy] checkout. Trigger: projectA, target: 7.x, checkout project: projectA"() {
        setup:
        def trigger = 'projectA'
        def target = '7.x'
        def mapping = '7.x'
        def checkoutProject = trigger

        def env = [:]
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = target
        env['ghprbGhRepository'] = trigger
        groovyScript.getBinding().setVariable("CHANGE_FORK", 'ginxo')

        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.checkoutProject(checkoutProject, 'kiegroup')
        then:
        if (trigger == checkoutProject) {
            1 * getPipelineMock("githubscm.mergeSourceIntoTarget")(checkoutProject, 'ginxo', 'branch1', 'kiegroup', mapping)
        } else {
            1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
            1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
                return projectBranchMappingProperties
            }
            1 * getPipelineMock("githubscm.checkoutIfExists")(checkoutProject, 'ginxo', 'branch1', 'kiegroup', mapping, true)
        }

        1 * getPipelineMock('githubscm.getCommit')() >> 'kiegroup/lienzo-core: 0f917d4 Expose zoom and pan filters (#102)'
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/master'
        1 * getPipelineMock('githubscm.getRemoteInfo')('origin', 'url') >> 'https://github.com/kiegroup/lienzo-core.git'
    }

    def "[util.groovy] checkout. Trigger: projectA, target: master, checkout project: optaplanner"() {
        setup:
        def trigger = 'projectA'
        def target = 'master'
        def mapping = '7.x'
        def checkoutProject = 'optaplanner'

        def env = [:]
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = target
        env['ghprbGhRepository'] = trigger
        groovyScript.getBinding().setVariable("CHANGE_FORK", 'ginxo')

        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.checkoutProject(checkoutProject, 'kiegroup')
        then:
        if (trigger == checkoutProject) {
            1 * getPipelineMock("githubscm.mergeSourceIntoTarget")(checkoutProject, 'ginxo', 'branch1', 'kiegroup', mapping)
        } else {
            1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
            1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
                return projectBranchMappingProperties
            }
            1 * getPipelineMock("githubscm.checkoutIfExists")(checkoutProject, 'ginxo', 'branch1', 'kiegroup', mapping, true)
        }

        1 * getPipelineMock('githubscm.getCommit')() >> 'kiegroup/lienzo-core: 0f917d4 Expose zoom and pan filters (#102)'
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/master'
        1 * getPipelineMock('githubscm.getRemoteInfo')('origin', 'url') >> 'https://github.com/kiegroup/lienzo-core.git'
    }

    def "[util.groovy] checkout. Trigger: projectA, target: 7.x, checkout project: optaplanner"() {
        setup:
        def trigger = 'projectA'
        def target = '7.x'
        def mapping = '7.x'
        def checkoutProject = 'optaplanner'

        def env = [:]
        env['CHANGE_AUTHOR'] = 'ginxo'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = target
        env['ghprbGhRepository'] = trigger
        groovyScript.getBinding().setVariable("CHANGE_FORK", 'ginxo')

        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.checkoutProject(checkoutProject, 'kiegroup')
        then:
        if (trigger == checkoutProject) {
            1 * getPipelineMock("githubscm.mergeSourceIntoTarget")(checkoutProject, 'ginxo', 'branch1', 'kiegroup', mapping)
        } else {
            1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
            1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
                return projectBranchMappingProperties
            }
            1 * getPipelineMock("githubscm.checkoutIfExists")(checkoutProject, 'ginxo', 'branch1', 'kiegroup', mapping, true)
        }

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
        this.getClass().getResource('/goals.properties').withInputStream {
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

    def "[util.groovy] getProjectTriggeringJob ghprbGhRepository present"() {
        setup:
        def env = [:]
        env['ghprbGhRepository'] = 'group/name1'
        groovyScript.getBinding().setVariable("env", env)
        when:
        def result = groovyScript.getProjectTriggeringJob()
        then:
        result[0] == 'group'
        result[1] == 'name1'
    }

    def "[util.groovy] getProjectTriggeringJob ghprbGhRepository not present but GITHUB_URL"() {
        setup:
        def env = [:]
        env['GIT_URL'] = 'https://github.com/jboss-integration/rhba.git'
        groovyScript.getBinding().setVariable("env", env)
        when:
        def result = groovyScript.getProjectTriggeringJob()
        then:
        result[0] == 'jboss-integration'
        result[1] == 'rhba'
    }

    def "[util.groovy] getProjectTriggeringJob ghprbGhRepository and GITHUB_URL not present"() {
        when:
        groovyScript.getProjectTriggeringJob()
        then:
        thrown(Exception)
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