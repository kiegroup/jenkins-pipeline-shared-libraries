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

    def "[util.groovy] checkout. Trigger: Optaplanner, target: main, checkout project: optaplanner"() {
        setup:
        def trigger = 'optaplanner'
        def target = 'main'
        def mapping = 'main'
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
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/main'
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
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/main'
        1 * getPipelineMock('githubscm.getRemoteInfo')('origin', 'url') >> 'https://github.com/kiegroup/lienzo-core.git'
    }

    def "[util.groovy] checkout. Trigger: Optaplanner, target: main, checkout project: projectA"() {
        setup:
        def trigger = 'optaplanner'
        def target = 'main'
        def mapping = 'main'
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
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/main'
        1 * getPipelineMock('githubscm.getRemoteInfo')('origin', 'url') >> 'https://github.com/kiegroup/lienzo-core.git'
    }

    def "[util.groovy] checkout. Trigger: Optaplanner, target: 7.x, checkout project: projectA"() {
        setup:
        def trigger = 'optaplanner'
        def target = '7.x'
        def mapping = 'main'
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
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/main'
        1 * getPipelineMock('githubscm.getRemoteInfo')('origin', 'url') >> 'https://github.com/kiegroup/lienzo-core.git'
    }

    def "[util.groovy] checkout. Trigger: projectA, target: main, checkout project: projectA"() {
        setup:
        def trigger = 'projectA'
        def target = 'main'
        def mapping = 'main'
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
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/main'
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
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/main'
        1 * getPipelineMock('githubscm.getRemoteInfo')('origin', 'url') >> 'https://github.com/kiegroup/lienzo-core.git'
    }

    def "[util.groovy] checkout. Trigger: projectA, target: main, checkout project: optaplanner"() {
        setup:
        def trigger = 'projectA'
        def target = 'main'
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
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/main'
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
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/main'
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

    def "[util.groovy] storeGitInformation no previous values"() {
        setup:
        def projectGroupName = ['group', 'name']
        def env = [:]
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.storeGitInformation('projectName')
        then:
        1 * getPipelineMock('githubscm.getCommit')() >> 'kiegroup/lienzo-core: 0f917d4 Expose zoom and pan filters (#102)'
        1 * getPipelineMock('githubscm.getCommitHash')() >> 'ac36137f12d1bcfa5cdf02b796a1a33d251b48e1'
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/main'
        1 * getPipelineMock('githubscm.getRemoteInfo')('origin', 'url') >> 'https://github.com/kiegroup/lienzo-core.git'
        env['GIT_INFORMATION_REPORT'] == "projectName=kiegroup/lienzo-core: 0f917d4 Expose zoom and pan filters (#102) Branch [* (detached from 0f917d4)  remotes/origin/main] Remote [https://github.com/kiegroup/lienzo-core.git]"
        env['GIT_INFORMATION_HASHES'] == "projectName=ac36137f12d1bcfa5cdf02b796a1a33d251b48e1"
    }

    def "[util.groovy] storeGitInformation with previous values"() {
        setup:
        def projectGroupName = ['group', 'name']
        def env = [:]
        env['GIT_INFORMATION_REPORT'] = 'projectName=kiegroup/lienzo-tests: 45c16e1 Fix tests (#84) Branch [* (detached from 45c16e1)  remotes/origin/main] Remote [https://github.com/kiegroup/lienzo-tests.git]'
        env['GIT_INFORMATION_HASHES'] = 'projectName=45c16e1'
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.storeGitInformation('projectName')
        then:
        1 * getPipelineMock('githubscm.getCommit')() >> 'kiegroup/lienzo-core: 0f917d4 Expose zoom and pan filters (#102)'
        1 * getPipelineMock('githubscm.getCommitHash')() >> '11111111111111111111111111111111'
        1 * getPipelineMock('githubscm.getBranch')() >> '* (detached from 0f917d4)  remotes/origin/main'
        1 * getPipelineMock('githubscm.getRemoteInfo')('origin', 'url') >> 'https://github.com/kiegroup/lienzo-core.git'
        env['GIT_INFORMATION_REPORT'] == 'projectName=kiegroup/lienzo-tests: 45c16e1 Fix tests (#84) Branch [* (detached from 45c16e1)  remotes/origin/main] Remote [https://github.com/kiegroup/lienzo-tests.git]; projectName=kiegroup/lienzo-core: 0f917d4 Expose zoom and pan filters (#102) Branch [* (detached from 0f917d4)  remotes/origin/main] Remote [https://github.com/kiegroup/lienzo-core.git]'
        env['GIT_INFORMATION_HASHES'] == 'projectName=45c16e1;projectName=11111111111111111111111111111111'
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
        when:
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

    def "[util.groovy] getReleaseBranchFromVersion"() {
        when:
        def output = groovyScript.getReleaseBranchFromVersion('1.50.425.Final')
        then:
        output == '1.50.x'
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

    def "[util.groovy] generateTempFile"() {
        when:
        def result = groovyScript.generateTempFile()
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: 'mktemp']) >> 'file'
        result == 'file'
    }

    def "[util.groovy] generateTempFolder"() {
        when:
        def result = groovyScript.generateTempFolder()
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: 'mktemp -d']) >> 'folder'
        result == 'folder'
    }

    def "[util.groovy] executeWithCredentialsMap with token"() {
        when:
        groovyScript.executeWithCredentialsMap([token: 'TOKEN']) {
            sh 'hello'
        }
        then:
        1 * getPipelineMock('string.call')([credentialsId: 'TOKEN', variable: 'QUAY_TOKEN']) >> 'token'
        1 * getPipelineMock('withCredentials')(['token'], _ as Closure)
        1 * getPipelineMock("sh")('hello')
        0 * getPipelineMock('error').call('No credentials given to execute the given closure')
    }

    def "[util.groovy] executeWithCredentialsMap with usernamePassword"() {
        when:
        groovyScript.executeWithCredentialsMap([usernamePassword: 'USERNAME_PASSWORD']) {
            sh 'hello'
        }
        then:
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'USERNAME_PASSWORD', usernameVariable: 'QUAY_USER', passwordVariable: 'QUAY_TOKEN']) >> 'usernamePassword'
        1 * getPipelineMock('withCredentials')(['usernamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('hello')
        0 * getPipelineMock('error').call('No credentials given to execute the given closure')
    }

    def "[util.groovy] executeWithCredentialsMap with token and usernamePassword"() {
        when:
        groovyScript.executeWithCredentialsMap([token: 'TOKEN', usernamePassword: 'USERNAME_PASSWORD']) {
            sh 'hello'
        }
        then:
        1 * getPipelineMock('string.call')([credentialsId: 'TOKEN', variable: 'QUAY_TOKEN']) >> 'token'
        1 * getPipelineMock('withCredentials')(['token'], _ as Closure)
        1 * getPipelineMock("sh")('hello')
        0 * getPipelineMock('error').call('No credentials given to execute the given closure')
    }

    def "[util.groovy] executeWithCredentialsMap empty"() {
        when:
        groovyScript.executeWithCredentialsMap([:]) {
            sh 'hello'
        }
        then:
        0 * getPipelineMock('string.call')([credentialsId: 'TOKEN', variable: 'QUAY_TOKEN']) >> 'token'
        0 * getPipelineMock('withCredentials')(['token'], _ as Closure)
        0 * getPipelineMock('usernamePassword.call')([credentialsId: 'USERNAME_PASSWORD', usernameVariable: 'QUAY_USER', passwordVariable: 'QUAY_TOKEN']) >> 'usernamePassword'
        0 * getPipelineMock('withCredentials')(['usernamePassword'], _ as Closure)
        0 * getPipelineMock("sh")('hello')
        1 * getPipelineMock('error').call('No credentials given to execute the given closure')
    }

    def "[util.groovy] cleanNode"() {
        when:
        groovyScript.cleanNode()
        then:
        0 * getPipelineMock('cloud.cleanContainersAndImages')(_)
        1 * getPipelineMock('maven.cleanRepository')()
        1 * getPipelineMock('cleanWs.call')()
    }

    def "[util.groovy] cleanNode with docker"() {
        when:
        groovyScript.cleanNode('docker')
        then:
        1 * getPipelineMock('cloud.cleanContainersAndImages')('docker')
        1 * getPipelineMock('maven.cleanRepository')()
        1 * getPipelineMock('cleanWs.call')()
    }

    def "[util.groovy] cleanNode with podman"() {
        when:
        groovyScript.cleanNode('podman')
        then:
        1 * getPipelineMock('cloud.cleanContainersAndImages')('podman')
        1 * getPipelineMock('maven.cleanRepository')()
        1 * getPipelineMock('cleanWs.call')()
    }

    def "[util.groovy] replaceInAllFilesRecursive"() {
        when:
        groovyScript.replaceInAllFilesRecursive('pattern*', 'sedpatternval\\', 'newValue')
        then:
        1 * getPipelineMock('sh')('find . -name \'pattern*\' -type f -exec sed -i \'s/sedpatternval\\/newValue/g\' {} \\;')
    }

    def "[util.groovy] rmPartialDeps"() {
        setup:
        def env = [:]
        env.put('WORKSPACE', '/workspacefolderrmPartialDeps')
        groovyScript.getBinding().setVariable("env", env)

        when:
        groovyScript.rmPartialDeps()
        then:
        1 * getPipelineMock('dir')('/workspacefolderrmPartialDeps/.m2', _ as Closure)
        1 * getPipelineMock("sh").call('find . -regex ".*\\.part\\(\\.lock\\)?" -exec rm -rf {} \\;')
    }



    def "[util.groovy] retrieveConsoleLog no arg"() {
        setup:
        groovyScript.getBinding().setVariable('BUILD_URL', 'URL/')
        when:
        def result = groovyScript.retrieveConsoleLog()
        then:
        1 * getPipelineMock('sh')([returnStdout: true, script: 'wget --no-check-certificate -qO - URL/consoleText | tail -n 100']) >> 'CONTENT'
        result == 'CONTENT'
    }

    def "[util.groovy] retrieveConsoleLog with number of lines"() {
        setup:
        groovyScript.getBinding().setVariable('BUILD_URL', 'URL/')
        when:
        def result = groovyScript.retrieveConsoleLog(3)
        then:
        1 * getPipelineMock('sh')([returnStdout: true, script: 'wget --no-check-certificate -qO - URL/consoleText | tail -n 3']) >> 'CONTENT'
        result == 'CONTENT'
    }

    def "[util.groovy] retrieveConsoleLog with number of lines and build url"() {
        setup:
        groovyScript.getBinding().setVariable('BUILD_URL', 'URL/')
        when:
        def result = groovyScript.retrieveConsoleLog(2, "BUILD_URL/")
        then:
        1 * getPipelineMock('sh')([returnStdout: true, script: 'wget --no-check-certificate -qO - BUILD_URL/consoleText | tail -n 2']) >> 'CONTENT'
        result == 'CONTENT'
    }

    def "[util.groovy] archiveConsoleLog no arg"() {
        setup:
        groovyScript.getBinding().setVariable('BUILD_URL', 'URL/')
        when:
        def result = groovyScript.archiveConsoleLog()
        then:
        1 * getPipelineMock('sh')('rm -rf console.log')
        1 * getPipelineMock('sh')([returnStdout: true, script: 'wget --no-check-certificate -qO - URL/consoleText | tail -n 100']) >> 'CONTENT'
        1 * getPipelineMock('writeFile')([text: 'CONTENT', file: 'console.log'])
        1 * getPipelineMock('archiveArtifacts.call')([artifacts: 'console.log'])
    }

    def "[util.groovy] archiveConsoleLog  with number of lines"() {
        setup:
        groovyScript.getBinding().setVariable('BUILD_URL', 'URL/')
        when:
        def result = groovyScript.archiveConsoleLog(3)
        then:
        1 * getPipelineMock('sh')('rm -rf console.log')
        1 * getPipelineMock('sh')([returnStdout: true, script: 'wget --no-check-certificate -qO - URL/consoleText | tail -n 3']) >> 'CONTENT'
        1 * getPipelineMock('writeFile')([text: 'CONTENT', file: 'console.log'])
        1 * getPipelineMock('archiveArtifacts.call')([artifacts: 'console.log'])
    }

    def "[util.groovy] archiveConsoleLog with number of lines and build url"() {
        setup:
        groovyScript.getBinding().setVariable('BUILD_URL', 'URL/')
        when:
        def result = groovyScript.archiveConsoleLog(3, 'BUILD_URL/')
        then:
        1 * getPipelineMock('sh')('rm -rf console.log')
        1 * getPipelineMock('sh')([returnStdout: true, script: 'wget --no-check-certificate -qO - BUILD_URL/consoleText | tail -n 3']) >> 'CONTENT'
        1 * getPipelineMock('writeFile')([text: 'CONTENT', file: 'console.log'])
        1 * getPipelineMock('archiveArtifacts.call')([artifacts: 'console.log'])
    }

    def "[util.groovy] retrieveTestResults no arg"() {
        setup:
        groovyScript.getBinding().setVariable('BUILD_URL', 'URL/')
        when:
        def result = groovyScript.retrieveTestResults()
        then:
        1 * getPipelineMock('sh')([returnStdout: true, script: 'wget --no-check-certificate -qO - URL/testReport/api/json']) >> 'CONTENT'
        1 * getPipelineMock('readJSON')([text: 'CONTENT']) >> [ hello : 'anything' ]
        result.hello == 'anything'
    }

    def "[util.groovy] retrieveTestResults with build url"() {
        setup:
        groovyScript.getBinding().setVariable('BUILD_URL', 'URL/')
        when:
        def result = groovyScript.retrieveTestResults("BUILD_URL/")
        then:
        1 * getPipelineMock('sh')([returnStdout: true, script: 'wget --no-check-certificate -qO - BUILD_URL/testReport/api/json']) >> 'CONTENT'
        1 * getPipelineMock('readJSON')([text: 'CONTENT']) >> [ hello : 'anything' ]
        result.hello == 'anything'
    }

    def "[util.groovy] retrieveFailedTests no arg"() {
        setup:
        groovyScript.getBinding().setVariable('BUILD_URL', 'URL/')
        def failedTests = [ 
            suites: [ 
                [ 
                    cases: [
                        [
                            status: 'FAILED',
                            className: 'package1.class1',
                            name: 'test'
                        ],
                        [
                            status: 'SKIPPED',
                            className: 'package1.class2.',
                            name: 'test'
                        ]
                    ]
                ],
                [ 
                    cases: [
                        [
                            status: 'FAILED',
                            className: 'package2.class1',
                            name: '(?)'
                        ],
                        [
                            status: 'PASSED',
                            className: 'package2.class2',
                            name: 'test'
                        ],
                        [
                            status: 'REGRESSION',
                            className: 'package2.class2',
                            name: 'test2'
                        ],
                    ]
                ] 
            ]
        ]
        when:
        def result = groovyScript.retrieveFailedTests()
        then:
        1 * getPipelineMock('sh')([returnStdout: true, script: 'wget --no-check-certificate -qO - URL/testReport/api/json']) >> 'CONTENT'
        1 * getPipelineMock('readJSON')([text: 'CONTENT']) >> failedTests
        result.size() == 3
        result.find { it.packageName ==  'package1' && it.className == 'class1' && it.name == 'test'}
        result.find { it.packageName ==  'package1' && it.className == 'class1' && it.name == 'test'}.fullName == 'package1.class1.test'
        result.find { it.packageName ==  'package1' && it.className == 'class1' && it.name == 'test'}.url == 'URL/testReport/package1/class1/test/'

        result.find { it.packageName ==  'package2' && it.className == 'class1' && it.name == '(?)'}
        result.find { it.packageName ==  'package2' && it.className == 'class1' && it.name == '(?)'}.fullName == 'package2.class1.(?)'
        result.find { it.packageName ==  'package2' && it.className == 'class1' && it.name == '(?)'}.url == 'URL/testReport/package2/class1/___/'

        result.find { it.packageName ==  'package2' && it.className == 'class2' && it.name == 'test2'}
        result.find { it.packageName ==  'package2' && it.className == 'class2' && it.name == 'test2'}.fullName == 'package2.class2.test2'
        result.find { it.packageName ==  'package2' && it.className == 'class2' && it.name == 'test2'}.url == 'URL/testReport/package2/class2/test2/'
    }

    def "[util.groovy] retrieveFailedTests with build url"() {
        setup:
        groovyScript.getBinding().setVariable('BUILD_URL/', 'URL/')
        def failedTests = [ 
            suites: [ 
                [ 
                    cases: [
                        [
                            status: 'FAILED',
                            className: 'package1.class1',
                            name: 'test'
                        ],
                        [
                            status: 'SKIPPED',
                            className: 'package1.class2.',
                            name: 'test'
                        ]
                    ]
                ],
                [ 
                    cases: [
                        [
                            status: 'FAILED',
                            className: 'package2.class1',
                            name: '(?)'
                        ],
                        [
                            status: 'PASSED',
                            className: 'package2.class2',
                            name: 'test'
                        ],
                        [
                            status: 'REGRESSION',
                            className: 'package2.class2',
                            name: 'test2'
                        ],
                    ]
                ] 
            ]
        ]
        when:
        def result = groovyScript.retrieveFailedTests('BUILD_URL/')
        then:
        1 * getPipelineMock('sh')([returnStdout: true, script: 'wget --no-check-certificate -qO - BUILD_URL/testReport/api/json']) >> 'CONTENT'
        1 * getPipelineMock('readJSON')([text: 'CONTENT']) >> failedTests
        result.size() == 3
        
        result.find { it.packageName ==  'package1' && it.className == 'class1' && it.name == 'test'}
        result.find { it.packageName ==  'package1' && it.className == 'class1' && it.name == 'test'}.fullName == 'package1.class1.test'
        result.find { it.packageName ==  'package1' && it.className == 'class1' && it.name == 'test'}.url == 'BUILD_URL/testReport/package1/class1/test/'

        result.find { it.packageName ==  'package2' && it.className == 'class1' && it.name == '(?)'}
        result.find { it.packageName ==  'package2' && it.className == 'class1' && it.name == '(?)'}.fullName == 'package2.class1.(?)'
        result.find { it.packageName ==  'package2' && it.className == 'class1' && it.name == '(?)'}.url == 'BUILD_URL/testReport/package2/class1/___/'

        result.find { it.packageName ==  'package2' && it.className == 'class2' && it.name == 'test2'}
        result.find { it.packageName ==  'package2' && it.className == 'class2' && it.name == 'test2'}.fullName == 'package2.class2.test2'
        result.find { it.packageName ==  'package2' && it.className == 'class2' && it.name == 'test2'}.url == 'BUILD_URL/testReport/package2/class2/test2/'
    }

    def "[util.groovy] retrieveArtifact default file exists"() {
        setup:
        groovyScript.getBinding().setVariable('BUILD_URL', 'URL/')
        when:
        def result = groovyScript.retrieveArtifact('PATH')
        then:
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -o /dev/null --silent -Iw '%{http_code}' URL/artifact/PATH"]) >> '200'
        1 * getPipelineMock('sh')([returnStdout: true, script: 'wget --no-check-certificate -qO - URL/artifact/PATH']) >> 'CONTENT'
        result == 'CONTENT'
    }


    def "[util.groovy] retrieveArtifact default file NOT exists"() {
        setup:
        groovyScript.getBinding().setVariable('BUILD_URL', 'URL/')
        when:
        def result = groovyScript.retrieveArtifact('PATH')
        then:
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -o /dev/null --silent -Iw '%{http_code}' URL/artifact/PATH"]) >> '404'
        0 * getPipelineMock('sh')([returnStdout: true, script: 'wget --no-check-certificate -qO - URL/artifact/PATH']) >> 'CONTENT'
        result == ''
    }


    def "[util.groovy] retrieveArtifact with build url and file exists"() {
        setup:
        groovyScript.getBinding().setVariable('BUILD_URL', 'URL/')
        when:
        def result = groovyScript.retrieveArtifact('PATH', 'BUILD_URL/')
        then:
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -o /dev/null --silent -Iw '%{http_code}' BUILD_URL/artifact/PATH"]) >> '200'
        1 * getPipelineMock('sh')([returnStdout: true, script: 'wget --no-check-certificate -qO - BUILD_URL/artifact/PATH']) >> 'CONTENT'
        result == 'CONTENT'
    }


    def "[util.groovy] retrieveArtifact with build url and file NOT exists"() {
        setup:
        groovyScript.getBinding().setVariable('BUILD_URL', 'URL/')
        when:
        def result = groovyScript.retrieveArtifact('PATH', 'BUILD_URL/')
        then:
        1 * getPipelineMock('sh')([returnStdout: true, script: "curl -o /dev/null --silent -Iw '%{http_code}' BUILD_URL/artifact/PATH"]) >> '404'
        0 * getPipelineMock('sh')([returnStdout: true, script: 'wget --no-check-certificate -qO - BUILD_URL/artifact/PATH']) >> 'CONTENT'
        result == ''
    }

    def "[util.groovy] retrieveJobInformation no arg"() {
        setup:
        groovyScript.getBinding().setVariable('BUILD_URL', 'URL/')
        def jobMock = [
            url: 'ANY_URL'
        ]
        when:
        def result = groovyScript.retrieveJobInformation()
        then:
        1 * getPipelineMock('sh')([returnStdout: true, script: 'wget --no-check-certificate -qO - URL/api/json']) >> 'CONTENT'
        1 * getPipelineMock('readJSON')([text: 'CONTENT']) >> jobMock
        result.url == 'ANY_URL'
    }

        def "[util.groovy] retrieveJobInformation with build url"() {
        setup:
        groovyScript.getBinding().setVariable('BUILD_URL', 'URL/')
        def jobMock = [
            url: 'ANY_URL'
        ]
        when:
        def result = groovyScript.retrieveJobInformation('BUILD_URL/')
        then:
        1 * getPipelineMock('sh')([returnStdout: true, script: 'wget --no-check-certificate -qO - BUILD_URL/api/json']) >> 'CONTENT'
        1 * getPipelineMock('readJSON')([text: 'CONTENT']) >> jobMock
        result.url == 'ANY_URL'
    }
}
