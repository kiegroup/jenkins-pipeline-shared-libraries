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
        env['CHANGE_AUTHOR'] = 'whatevergroup'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = target
        env['ghprbGhRepository'] = trigger
        env['ghprbAuthorRepoGitUrl'] = 'https://github.com/sourceauthor/projectx.git'
        groovyScript.getBinding().setVariable("CHANGE_FORK", 'whatevergroup')

        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.checkoutProject(checkoutProject, 'kiegroup')
        then:
        if (trigger == checkoutProject) {
            1 * getPipelineMock('githubscm.getForkedProjectName')('kiegroup', checkoutProject, 'sourceauthor') >> 'forkedname'
            1 * getPipelineMock("githubscm.mergeSourceIntoTarget")('forkedname', 'sourceauthor', 'branch1', checkoutProject, 'kiegroup', mapping)
        } else {
            1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
            1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
                return projectBranchMappingProperties
            }
            1 * getPipelineMock("githubscm.checkoutIfExists")(checkoutProject, 'sourceauthor', 'branch1', 'kiegroup', mapping, true)
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
        env['CHANGE_AUTHOR'] = 'whatevergroup'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = target
        env['GIT_URL'] = "https://github.com/whatevergroup/optaplanner.git"
        env['ghprbGhRepository'] = trigger
        groovyScript.getBinding().setVariable("CHANGE_FORK", 'whatevergroup')

        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.checkoutProject(checkoutProject, 'kiegroup')
        then:
        if (trigger == checkoutProject) {
            1 * getPipelineMock('githubscm.getForkedProjectName')('kiegroup', checkoutProject, 'whatevergroup') >> 'forkedname'
            1 * getPipelineMock("githubscm.mergeSourceIntoTarget")('forkedname', 'whatevergroup', 'branch1', checkoutProject, 'kiegroup', mapping)
        } else {
            1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
            1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
                return projectBranchMappingProperties
            }
            1 * getPipelineMock("githubscm.checkoutIfExists")(checkoutProject, 'whatevergroup', 'branch1', 'kiegroup', mapping, true)
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
        env['CHANGE_AUTHOR'] = 'whatevergroup'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = target
        env['ghprbGhRepository'] = trigger
        env['ghprbAuthorRepoGitUrl'] = 'https://github.com/sourceauthor/projectx.git'
        groovyScript.getBinding().setVariable("CHANGE_FORK", 'whatevergroup')

        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.checkoutProject(checkoutProject, 'kiegroup')
        then:
        if (trigger == checkoutProject) {
            1 * getPipelineMock('githubscm.getForkedProjectName')('kiegroup', checkoutProject, 'sourceauthor') >> 'forkedname'
            1 * getPipelineMock("githubscm.mergeSourceIntoTarget")('forkedname', 'sourceauthor', 'branch1', checkoutProject, 'kiegroup', mapping)
        } else {
            1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
            1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
                return projectBranchMappingProperties
            }
            1 * getPipelineMock("githubscm.checkoutIfExists")(checkoutProject, 'sourceauthor', 'branch1', 'kiegroup', mapping, true)
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
        env['CHANGE_AUTHOR'] = 'whatevergroup'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = target
        env['ghprbGhRepository'] = trigger
        env['GIT_URL'] = "https://github.com/whatevergroup/optaplanner.git"

        groovyScript.getBinding().setVariable("CHANGE_FORK", 'whatevergroup')

        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.checkoutProject(checkoutProject, 'kiegroup')
        then:
        if (trigger == checkoutProject) {
            1 * getPipelineMock('githubscm.getForkedProjectName')('kiegroup', checkoutProject, 'whatevergroup') >> 'forkedname'
            1 * getPipelineMock("githubscm.mergeSourceIntoTarget")('forkedname', 'whatevergroup', 'branch1', checkoutProject, 'kiegroup', mapping)
        } else {
            1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
            1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
                return projectBranchMappingProperties
            }
            1 * getPipelineMock("githubscm.checkoutIfExists")(checkoutProject, 'whatevergroup', 'branch1', 'kiegroup', mapping, true)
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
        env['CHANGE_AUTHOR'] = 'whatevergroup'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = target
        env['ghprbGhRepository'] = trigger
        env['GIT_URL'] = "https://github.com/whatevergroup/projectA.git"
        groovyScript.getBinding().setVariable("CHANGE_FORK", 'whatevergroup')

        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.checkoutProject(checkoutProject, 'kiegroup')
        then:
        if (trigger == checkoutProject) {
            1 * getPipelineMock('githubscm.getForkedProjectName')('kiegroup', checkoutProject, 'whatevergroup') >> 'forkedname'
            1 * getPipelineMock("githubscm.mergeSourceIntoTarget")('forkedname', 'whatevergroup', 'branch1', checkoutProject, 'kiegroup', mapping)
        } else {
            1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
            1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
                return projectBranchMappingProperties
            }
            1 * getPipelineMock("githubscm.checkoutIfExists")(checkoutProject, 'whatevergroup', 'branch1', 'kiegroup', mapping, true)
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
        env['CHANGE_AUTHOR'] = 'whatevergroup'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = target
        env['ghprbGhRepository'] = trigger
        env['GIT_URL'] = "https://github.com/whatevergroup/projectA.git"
        groovyScript.getBinding().setVariable("CHANGE_FORK", 'whatevergroup')

        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.checkoutProject(checkoutProject, 'kiegroup')
        then:
        if (trigger == checkoutProject) {
            1 * getPipelineMock('githubscm.getForkedProjectName')('kiegroup', checkoutProject, 'whatevergroup') >> 'forkedname'
            1 * getPipelineMock("githubscm.mergeSourceIntoTarget")('forkedname', 'whatevergroup', 'branch1', checkoutProject, 'kiegroup', mapping)
        } else {
            1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
            1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
                return projectBranchMappingProperties
            }
            1 * getPipelineMock("githubscm.checkoutIfExists")(checkoutProject, 'whatevergroup', 'branch1', 'kiegroup', mapping, true)
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
        env['CHANGE_AUTHOR'] = 'whatevergroup'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = target
        env['ghprbGhRepository'] = trigger
        env['GIT_URL'] = "https://github.com/whatevergroup/projectA.git"
        groovyScript.getBinding().setVariable("CHANGE_FORK", 'whatevergroup')

        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.checkoutProject(checkoutProject, 'kiegroup')
        then:
        if (trigger == checkoutProject) {
            1 * getPipelineMock('githubscm.getForkedProjectName')('kiegroup', checkoutProject, 'whatevergroup') >> 'forkedname'
            1 * getPipelineMock("githubscm.mergeSourceIntoTarget")('forkedname', 'whatevergroup', 'branch1', checkoutProject, 'kiegroup', mapping)
        } else {
            1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
            1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
                return projectBranchMappingProperties
            }
            1 * getPipelineMock("githubscm.checkoutIfExists")(checkoutProject, 'whatevergroup', 'branch1', 'kiegroup', mapping, true)
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
        env['CHANGE_AUTHOR'] = 'whatevergroup'
        env['CHANGE_BRANCH'] = 'branch1'
        env['CHANGE_TARGET'] = target
        env['ghprbGhRepository'] = trigger
        env['GIT_URL'] = "https://github.com/whatevergroup/projectA.git"
        groovyScript.getBinding().setVariable("CHANGE_FORK", 'whatevergroup')

        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.checkoutProject(checkoutProject, 'kiegroup')
        then:
        if (trigger == checkoutProject) {
            1 * getPipelineMock('githubscm.getForkedProjectName')('kiegroup', checkoutProject, 'whatevergroup') >> 'forkedname'
            1 * getPipelineMock("githubscm.mergeSourceIntoTarget")('forkedname', 'whatevergroup', 'branch1', checkoutProject, 'kiegroup', mapping)
        } else {
            1 * getPipelineMock("configFile.call")(['fileId': 'project-branches-mapping', 'variable': 'PROPERTIES_FILE']) >> { return 'project-branches-mapping.properties' }
            1 * getPipelineMock("readProperties")(['file': 'project-branches-mapping.properties']) >> {
                return projectBranchMappingProperties
            }
            1 * getPipelineMock("githubscm.checkoutIfExists")(checkoutProject, 'whatevergroup', 'branch1', 'kiegroup', mapping, true)
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
        def filePath = "goals.properties"
        def url = getClass().getResource(filePath)
        def fileContent = new File(url.toURI()).text
        Properties properties = new Properties()
        properties.load(new StringReader(fileContent))

        when:
        def goals = groovyScript.getGoals('project1', filePath, 'typex')
        then:
        1 * getPipelineMock("readProperties")(['file': filePath]) >> {
            return properties
        }
        goals == "typexValue"
    }

    def "[util.groovy] getGoals existing in env folder"() {
        setup:
        def env = [:]
        env.put('WORKSPACE', '/workspacefolder')
        groovyScript.getBinding().setVariable("env", env)

        def filePath = "goals.properties"
        def url = getClass().getResource(filePath)
        def fileContent = new File(url.toURI()).text
        Properties properties = new Properties()
        properties.load(new StringReader(fileContent))

        when:
        def goals = groovyScript.getGoals('project1', "/workspacefolder/${filePath}", 'typex')
        then:
        1 * getPipelineMock("readProperties")(['file': "/workspacefolder/${filePath}"]) >> null
        1 * getPipelineMock("readProperties")(['file': "/workspacefolder/.ci-env/${filePath}"]) >> properties
        goals == "typexValue"
    }

    def "[util.groovy] getGoals not existing"() {
        setup:
        def env = [:]
        env.put('WORKSPACE', '/workspacefolder')
        groovyScript.getBinding().setVariable("env", env)

        def filePath = "goals.properties"
        def url = getClass().getResource(filePath)
        def fileContent = new File(url.toURI()).text
        Properties properties = new Properties()
        properties.load(new StringReader(fileContent))

        when:
        groovyScript.getGoals('project1', "/workspacefolder/${filePath}", 'typex')
        then:
        1 * getPipelineMock("readProperties")(['file': "/workspacefolder/${filePath}"]) >> null
        1 * getPipelineMock("readProperties")(['file': "/workspacefolder/.ci-env/${filePath}"]) >> null
        thrown(AssertionError)
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

    def "[util.groovy] getProjectDirPath without group"() {
        setup:
        def projectGroupName = ['group', 'name']
        def env = [:]
        env.put('WORKSPACE', '/workspacefolder')
        groovyScript.getBinding().setVariable("env", env)
        when:
        def result = groovyScript.getProjectDirPath('projectA')
        then:
        result == "/workspacefolder/kiegroup_projectA"
    }

    def "[util.groovy] getNextVersionMicro"() {
        when:
        def snapshotVersion = groovyScript.getNextVersion('0.12.0', 'micro')
        then:
        '0.12.1-SNAPSHOT' == snapshotVersion
        
    }

    def "[util.groovy] getNextVersionMinor"() {
        when:
        def snapshotVersion = groovyScript.getNextVersion('0.12.0', 'minor')
        then:
        '0.13.0-SNAPSHOT' == snapshotVersion
        
    }

    def "[util.groovy] getNextVersionMajor"() {
        when:
        def snapshotVersion = groovyScript.getNextVersion('0.12.0', 'major')
        then:
        '1.12.0-SNAPSHOT' == snapshotVersion
    }

    def "[util.groovy] getNextVersionSuffixTest"() {
        when:
        def snapshotVersion = groovyScript.getNextVersion('0.12.0', 'minor', 'whatever')
        then:
        '0.13.0-whatever' == snapshotVersion       
    }

    def "[util.groovy] getNextVersionErrorContainsAlphabets"() {
        when:
        def checkForAlphabets = groovyScript.getNextVersion('a.12.0', 'micro')
        then:
        1 * getPipelineMock("error").call('Version a.12.0 is not in the required format. The major, minor, and micro parts should contain only numeric characters.')
    }
    
    def "[util.groovy] getNextVersionErrorFormat"() {
        when:
        def checkForFormatError = groovyScript.getNextVersion('0.12.0.1', 'micro')
        then:
        1 * getPipelineMock("error").call('Version 0.12.0.1 is not in the required format X.Y.Z or X.Y.Z.suffix.')
    }
    
    def "[util.groovy] getNextVersion null"() {
        when:
        def version = groovyScript.getNextVersion('0.12.0', 'micro', null)
        then:
        '0.12.1' == version
    }

    def "[util.groovy] getNextVersionAssertErrorCheck"() {
        when :
        groovyScript.getNextVersion('0.12.0', 'microo')
        then:
        thrown(AssertionError)
    } 

    def "[util.groovy] parseVersionCorrect"() {
        when:
        def version = groovyScript.parseVersion('0.12.6598')
        then:
        version[0] == 0
        version[1] == 12
        version[2] == 6598
    }

    def "[util.groovy] parseVersionWithSuffixCorrect"() {
        when:
        def version = groovyScript.parseVersion('1.0.0.Final')
        then:
        version[0] == 1
        version[1] == 0
        version[2] == 0
    }

    def "[util.groovy] parseVersionErrorContainsAlphabets"() {
        when:
        groovyScript.parseVersion('a.12.0')
        then:
        1 * getPipelineMock("error").call('Version a.12.0 is not in the required format. The major, minor, and micro parts should contain only numeric characters.')
    }
    
    def "[util.groovy] parseVersionErrorFormat"() {
        when:
        groovyScript.parseVersion('0.12.0.1')
        then:
        1 * getPipelineMock("error").call('Version 0.12.0.1 is not in the required format X.Y.Z or X.Y.Z.suffix.')
    }

    def "[util.groovy] generateHashSize9"() {
        when:
        def hash9 = groovyScript.generateHash(9)
        then:
        hash9.length() == 9
    }

    def "[util.groovy] generateHashSize1000"() {
        when:
        def hash1000 = groovyScript.generateHash(1000)
        then:
        hash1000.length() == 1000
    }
}