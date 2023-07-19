import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import groovy.json.JsonSlurper
import hudson.plugins.git.GitSCM

class GithubScmSpec extends JenkinsPipelineSpecification {
    def groovyScript = null
    def pullRequestInfo = null
    def pullRequestInfoEmpty = null
    def forkListInfo = null
    def forkListInfoPage3 = null
    def forkListInfoEmpty = null
    def forkListInfoMissingOwner = null
    def jsonSlurper = new JsonSlurper()

    def setup() {
        groovyScript = loadPipelineScriptForTest("vars/githubscm.groovy")

        // shared setup for tagRepository
        explicitlyMockPipelineVariable("out")
        getPipelineMock("sh")([returnStdout: true, script: 'git log --oneline -1']) >> {
            return 'commitIdMock'
        }

        // shared setup for pushObject
        explicitlyMockPipelineVariable("GITHUB_USER")
        explicitlyMockPipelineVariable("GITHUB_TOKEN")

        groovyScript.getBinding().setVariable("OAUTHTOKEN", 'oauth_token')
        pullRequestInfo = mockJson('/pull_request_not_empty.json')
        pullRequestInfoEmpty = mockJson('/pull_request_empty.json')
        forkListInfo = mockJson('/forked_projects.json')
        forkListInfoPage3 = mockJson('/forked_projects_page3.json')
        forkListInfoEmpty = mockJson('/forked_projects_empty.json')
        forkListInfoMissingOwner = mockJson('/forked_projects_missing_owner.json')

        getPipelineMock("sh")([returnStdout: true, script: 'mktemp -d']) >> {
            return 'tempDir'
        }
    }

    def mockJson(def fileName) {
        def url = getClass().getResource(fileName)
        def data = new File(url.toURI()).text
        getPipelineMock("readJSON")(['text': data]) >> jsonSlurper.parseText(data)
        return data
    }

    def "[githubscm.groovy] resolveRepository"() {
        when:
        def result = groovyScript.resolveRepository('repository', 'author', 'branches', true)
        then:
        result == [$class: "GitSCM", branches: [[name: "branches"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/author/repository.git"]]]
    }

    def "[githubscm.groovy] resolveRepository with different credentials"() {
        when:
        def result = groovyScript.resolveRepository('repository', 'author', 'branches', true, 'ci-usernamePassword')
        then:
        result == [$class: "GitSCM", branches: [[name: "branches"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "ci-usernamePassword", "url": "https://github.com/author/repository.git"]]]
    }

    def "[githubscm.groovy] checkoutIfExists without merge"() {
        setup:
        def gitSCM = [$class: "GitSCM", branches: [[name: "branches"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/author/repository.git"]]]
        when:
        groovyScript.checkoutIfExists('repository', 'author', 'branches', 'defaultAuthor', 'main')
        then:
        2 * getPipelineMock("checkout")(gitSCM)
        0 * getPipelineMock("sh")(['returnStdout': true, 'script': _])
    }

    def "[githubscm.groovy] checkoutIfExists first repo does not exist"() {
        setup:
        def gitSCM = [$class: "GitSCM", branches: [[name: "branches"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/author/repository.git"]]]
        when:
        groovyScript.checkoutIfExists('repository', 'author', 'branches', 'defaultAuthor', 'main')
        then:
        0 * getPipelineMock("checkout")(null)

        2 * getPipelineMock("checkout")(gitSCM)
        0 * getPipelineMock("sh")(['returnStdout': true, 'script': _])
    }

    def "[githubscm.groovy] checkoutIfExists with merge true"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        def repositoryScmInformation = [$class: "GitSCM", branches: [[name: "branches"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/author/repository.git"]]]
        def repositoryScmInformationMain = [$class: "GitSCM", branches: [[name: "main"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/defaultAuthor/repository.git"]]]
        when:
        groovyScript.checkoutIfExists('repository', 'author', 'branches', 'defaultAuthor', 'main', true)
        then:
        1 * getPipelineMock('checkout')(repositoryScmInformation)
        1 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'kie-ci', variable: 'kieCiUserPassword']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock('checkout')(repositoryScmInformationMain)
        1 * getPipelineMock('sh')('git pull https://user:password@github.com/author/repository branches')
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1']) >> 'git commit information'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl --globoff -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/pulls?head=author:branches&state=open'"]) >> pullRequestInfo
    }

    def "[githubscm.groovy] checkoutIfExists with merge true and different forked project name"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        def repositoryScmInformation = [$class: "GitSCM", branches: [[name: "branches"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/author/irtyamine.git"]]]
        def repositoryScmInformationMain = [$class: "GitSCM", branches: [[name: "main"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/defaultAuthor/repository.git"]]]
        when:
        groovyScript.checkoutIfExists('repository', 'irtyamine', 'branches', 'defaultAuthor', 'main', true)
        then:
        0 * getPipelineMock('checkout')(repositoryScmInformation)
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/forks?per_page=100&page=1'"]) >> forkListInfo
        1 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'kie-ci', variable: 'kieCiUserPassword']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock('checkout')(repositoryScmInformationMain)
        1 * getPipelineMock('sh')('git pull https://user:password@github.com/irtyamine/github-action-build-chain branches')
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1']) >> 'git commit information'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl --globoff -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/pulls?head=irtyamine:branches&state=open'"]) >> pullRequestInfo
    }

    def "[githubscm.groovy] checkoutIfExists with merge true and different credentials"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        def repositoryScmInformation = [$class: "GitSCM", branches: [[name: "branches"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "ci-usernamePassword", "url": "https://github.com/author/irtyamine.git"]]]
        def repositoryScmInformationMain = [$class: "GitSCM", branches: [[name: "main"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "ci-usernamePassword", "url": "https://github.com/defaultAuthor/repository.git"]]]
        when:
        groovyScript.checkoutIfExists('repository', 'author', 'branches', 'defaultAuthor', 'main', true, ['token': 'ci-token', 'usernamePassword': 'ci-usernamePassword'])
        then:
        0 * getPipelineMock('checkout')(repositoryScmInformation)
        1 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'ci-usernamePassword', variable: 'kieCiUserPassword']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock('checkout')(repositoryScmInformationMain)
        1 * getPipelineMock('sh')('git pull https://user:password@github.com/author/repository branches')
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1']) >> 'git commit information'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl --globoff -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/pulls?head=author:branches&state=open'"]) >> pullRequestInfo
        2 * getPipelineMock("string.call")(['credentialsId': 'ci-token', 'variable': 'OAUTHTOKEN'])
    }

    def "[githubscm.groovy] checkoutIfExists has not PR"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        def repositoryScmInformation = [$class: "GitSCM", branches: [[name: "branches"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/author/irtyamine.git"]]]
        def repositoryScmInformationMain = [$class: "GitSCM", branches: [[name: "main"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/defaultAuthor/repository.git"]]]
        when:
        groovyScript.checkoutIfExists('repository', 'author', 'branches', 'defaultAuthor', 'main', true)
        then:
        0 * getPipelineMock('checkout')(repositoryScmInformation)
        0 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'kie-ci', variable: 'kieCiUserPassword']) >> 'userNamePassword'

        1 * getPipelineMock('checkout')(repositoryScmInformationMain)
        0 * getPipelineMock('sh')('git pull https://user:password@github.com/author/repository branches')

        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl --globoff -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/pulls?head=author:branches&state=open'"]) >> pullRequestInfoEmpty
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl --globoff -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/pulls?head=defaultAuthor:branches&state=open'"]) >> pullRequestInfoEmpty
    }

    def "[githubscm.groovy] checkoutIfExists Multibranch pipeline job"() {
        setup:
        def gitSCM = [$class: "GitSCM", branches: [[name: "main"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/kiegroup/repository.git"]]]
        when:
        groovyScript.checkoutIfExists('repository', 'kiegroup', 'main', 'kiegroup', 'main')
        then:
        0 * getPipelineMock("sh")(['returnStdout': true, 'script': _])
        2 * getPipelineMock("checkout")(gitSCM)
        0 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1'])
    }

    def "[githubscm.groovy] getRepositoryScm"() {
        setup:
        def gitSCM = [$class: "GitSCM", branches: [[name: "branches"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/author/repository.git"]]]
        when:
        def result = groovyScript.getRepositoryScm('repository', 'author', 'branches')
        then:
        result == gitSCM
    }

    def "[githubscm.groovy] getRepositoryScm with different credentials"() {
        setup:
        def gitSCM = [$class: "GitSCM", branches: [[name: "branches"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "ci-usernamePassword", "url": "https://github.com/author/repository.git"]]]
        when:
        def result = groovyScript.getRepositoryScm('repository', 'author', 'branches', 'ci-usernamePassword')
        then:
        result == gitSCM
    }

    def "[githubscm.groovy] getRepositoryScm with non-existent branch"() {
        setup:
        def gitSCM = [$class: "GitSCM", branches: [[name: "branches"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/author/repository.git"]]]
        getPipelineMock('checkout')(gitSCM) >> { throw new Exception() }
        when:
        def result = groovyScript.getRepositoryScm('repository', 'author', 'branches')
        then:
        result == null
    }

    def "[githubscm.groovy] mergeSourceIntoTarget"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        def repositoryScmInformation = [$class: "GitSCM", branches: [[name: "targetBranches"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/targetAuthor/targetRepository.git"]]]
        when:
        groovyScript.mergeSourceIntoTarget('sourceRepository', 'sourceAuthor', 'sourceBranches', 'targetRepository', 'targetAuthor', 'targetBranches')
        then:
        1 * getPipelineMock('checkout')(repositoryScmInformation)
        1 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'kie-ci', variable: 'kieCiUserPassword']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock('sh')('git pull https://user:password@github.com/sourceAuthor/sourceRepository sourceBranches')
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1']) >> 'git commit information'
    }

    def "[githubscm.groovy] mergeSourceIntoTarget with different credentialsID"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        def repositoryScmInformation = [$class: "GitSCM", branches: [[name: "targetBranches"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "ci-usernamePassword", "url": "https://github.com/targetAuthor/targetRepository.git"]]]
        when:
        groovyScript.mergeSourceIntoTarget('sourceRepository', 'sourceAuthor', 'sourceBranches', 'targetRepository', 'targetAuthor', 'targetBranches', 'ci-usernamePassword')
        then:
        1 * getPipelineMock('checkout')(repositoryScmInformation)
        1 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'ci-usernamePassword', variable: 'kieCiUserPassword']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock('sh')('git pull https://user:password@github.com/sourceAuthor/sourceRepository sourceBranches')
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1']) >> 'git commit information'
    }

    def "[githubscm.groovy] mergeSourceIntoTarget throw exception"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        def repositoryScmInformation = [$class: "GitSCM", branches: [[name: "targetBranches"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/targetAuthor/targetRepository.git"]]]
        when:
        groovyScript.mergeSourceIntoTarget('sourceRepository', 'sourceAuthor', 'sourceBranches', 'targetRepository', 'targetAuthor', 'targetBranches')
        then:
        1 * getPipelineMock('checkout')(repositoryScmInformation)
        1 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'kie-ci', variable: 'kieCiUserPassword']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock('sh')('git pull https://user:password@github.com/sourceAuthor/sourceRepository sourceBranches') >> { throw new Exception('git error') }
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1']) >> 'git commit information'
        thrown(Exception)
    }

    def "[githubscm.groovy] createBranch"() {
        when:
        groovyScript.createBranch('branchName')
        then:
        1 * getPipelineMock("sh")('git checkout -b branchName')
    }

    def "[githubscm.groovy] createBranch exception"() {
        when:
        groovyScript.createBranch('branchName')
        then:
        1 * getPipelineMock("sh")('git checkout -b branchName') >> { throw new Exception('git error') }
        thrown(Exception)
    }

    def "[githubscm.groovy] isBranchExist ok"() {
        when:
        def result = groovyScript.isBranchExist('origin', 'BRANCH')
        then:
        1 * getPipelineMock("sh")('git fetch origin')
        1 * getPipelineMock("sh")([returnStatus: true, script: "git rev-parse BRANCH"]) >> 0
        result
    }

    def "[githubscm.groovy] isBranchExist not existing"() {
        when:
        def result = groovyScript.isBranchExist('origin', 'BRANCH')
        then:
        1 * getPipelineMock("sh")('git fetch origin')
        1 * getPipelineMock("sh")([returnStatus: true, script: "git rev-parse BRANCH"]) >> 1
        !result
    }

    def "[githubscm.groovy] removeRemoteBranch without credentialsId"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        groovyScript.removeRemoteBranch('remote', 'BRANCH')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'kie-ci', 'usernameVariable': 'GITHUB_USER', 'passwordVariable': 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")('git config --local credential.helper "!f() { echo username=\\user; echo password=\\password; }; f"')
        1 * getPipelineMock("sh")("git push --delete remote BRANCH")
    }

    def "[githubscm.groovy] removeRemoteBranch with credentialsId"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        groovyScript.removeRemoteBranch('remote', 'BRANCH', 'credsId')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'credsId', 'usernameVariable': 'GITHUB_USER', 'passwordVariable': 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")('git config --local credential.helper "!f() { echo username=\\user; echo password=\\password; }; f"')
        1 * getPipelineMock("sh")("git push --delete remote BRANCH")
    }

    def "[githubscm.groovy] removeLocalBranch"() {
        when:
        groovyScript.removeLocalBranch('BRANCH')
        then:
        1 * getPipelineMock("sh")("git branch -D BRANCH")
    }

    def "[githubscm.groovy] commitChanges without files to add"() {
        when:
        groovyScript.commitChanges('commit message')
        then:
        1 * getPipelineMock("sh")('git add -u')
        1 * getPipelineMock("sh")("git commit -m 'commit message'")
    }

    def "[githubscm.groovy] commitChanges with files to add"() {
        when:
        groovyScript.commitChanges('commit message', 'src/*')
        then:
        1 * getPipelineMock("sh")('git add src/*')
        1 * getPipelineMock("sh")("git commit -m 'commit message'")
    }

    def "[githubscm.groovy] commitChanges with precommit closure"() {
        when:
        groovyScript.commitChanges('commit message', {
            sh 'whatever'
        })
        then:
        1 * getPipelineMock("sh")('whatever')
        1 * getPipelineMock("sh")("git commit -m 'commit message'")
    }

    def "[githubscm.groovy] commitChanges without precommit closure"() {
        when:
        groovyScript.commitChanges('commit message')
        then:
        1 * getPipelineMock("sh")("git commit -m 'commit message'")
    }

    def "[githubscm.groovy] addRemote simple"() {
        when:
        groovyScript.addRemote('prod', 'https://github.com/kiegroup/droolsjbpm-build-bootstrap.git')
        then:
        1 * getPipelineMock("sh")("git remote add prod https://github.com/kiegroup/droolsjbpm-build-bootstrap.git")
    }

    def "[githubscm.groovy] squashCommits simple"() {
        when:
        groovyScript.squashCommits('main', 'COMMIT_MSG')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "git rev-parse --abbrev-ref HEAD"]) >> 'BRANCH'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "git merge-base main BRANCH"]) >> 'MERGE_NAME'
        1 * getPipelineMock("sh")("git reset MERGE_NAME")
        1 * getPipelineMock("sh")("git add -A")
        1 * getPipelineMock("sh")('git commit -m "COMMIT_MSG"')
    }


    def "[githubscm.groovy] forkRepo without credentials"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        groovyScript.forkRepo()
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'kie-ci', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)

        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")('git config hub.protocol https')
        1 * getPipelineMock("sh")('hub fork --remote-name=origin')
        1 * getPipelineMock("sh")('git remote -v')
    }

    def "[githubscm.groovy] forkRepo with credentials"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        groovyScript.forkRepo('credentialsId')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'credentialsId', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)

        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")('git config hub.protocol https')
        1 * getPipelineMock("sh")('hub fork --remote-name=origin')
        1 * getPipelineMock("sh")('git remote -v')
    }

    def "[githubscm.groovy] createPR without body, Credentials and target branch"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        def result = groovyScript.createPR('PR Title')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'kie-ci', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hub pull-request -m 'PR Title' -m '' -b 'main'"]) >> 'shResult'
    }

    def "[githubscm.groovy] createPR without Credentials and target branch"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        def result = groovyScript.createPR('PR Title', 'PR body.')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'kie-ci', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hub pull-request -m 'PR Title' -m 'PR body.' -b 'main'"]) >> 'shResult'
    }

    def "[githubscm.groovy] createPR without Credentials and target branch throwing exception"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        def result = groovyScript.createPR('PR Title')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'kie-ci', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hub pull-request -m 'PR Title' -m '' -b 'main'"]) >> { throw new Exception('error') }
        thrown(Exception)
    }

    def "[githubscm.groovy] createPR with body, Credentials and target branch"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        def result = groovyScript.createPR('PR Title', 'PR body.', 'targetBranch', 'credentialsId')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'credentialsId', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hub pull-request -m 'PR Title' -m 'PR body.' -b 'targetBranch'"]) >> 'shResult'
    }

    def "[githubscm.groovy] createPrAsDraft without body, Credentials and target branch"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        def result = groovyScript.createPrAsDraft('PR Title')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'kie-ci', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hub pull-request -d -m 'PR Title' -m '' -b 'main'"]) >> 'shResult'
    }

    def "[githubscm.groovy] createPrAsDraft without Credentials and target branch"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        def result = groovyScript.createPrAsDraft('PR Title', 'PR body.')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'kie-ci', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hub pull-request -d -m 'PR Title' -m 'PR body.' -b 'main'"]) >> 'shResult'
    }

    def "[githubscm.groovy] createPrAsDraft without Credentials and target branch throwing exception"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        def result = groovyScript.createPrAsDraft('PR Title')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'kie-ci', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hub pull-request -d -m 'PR Title' -m '' -b 'main'"]) >> { throw new Exception('draft error') }
        thrown(Exception)
    }

    def "[githubscm.groovy] createPrAsDraft with body, Credentials and target branch"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        def result = groovyScript.createPrAsDraft('PR Title', 'PR body.', 'targetBranch', 'credentialsId')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'credentialsId', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hub pull-request -d -m 'PR Title' -m 'PR body.' -b 'targetBranch'"]) >> 'shResult'
    }

    def "[githubscm.groovy] createPRWithLabels with body, Credentials and target branch"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        def result = groovyScript.createPRWithLabels('PR Title', 'PR body.', 'targetBranch', ['label1', 'label2'] as String[], 'credentialsId')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'credentialsId', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hub pull-request -m 'PR Title' -m 'PR body.' -b 'targetBranch' -l 'label1','label2'"]) >> 'shResult'
    }

    def "[githubscm.groovy] executeHub with credentials"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        def result = groovyScript.executeHub('hubCommand', 'credentialsId')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'credentialsId', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hubCommand"]) >> 'shResult'
    }

    def "[githubscm.groovy] mergePR without Credentials"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        groovyScript.mergePR('pullRequestLink')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'kie-ci', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")('hub merge pullRequestLink')
    }

    def "[githubscm.groovy] mergePR without Credentials throwing exception"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        groovyScript.mergePR('pullRequestLink')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'kie-ci', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")('hub merge pullRequestLink') >> { throw new Exception('hub error') }
        thrown(Exception)
    }

    def "[githubscm.groovy] mergePR with Credentials"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        groovyScript.mergePR('pullRequestLink', 'credentialsId')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'credentialsId', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")('hub merge pullRequestLink')
    }

    def "[githubscm.groovy] tagRepository with buildTag"() {
        when:
        groovyScript.tagRepository('tagName', 'buildTag')
        then:
        1 * getPipelineMock("sh")("git tag -a 'tagName' -m 'Tagged by Jenkins in build \"buildTag\".'")
    }

    def "[githubscm.groovy] tagRepository without buildTag"() {
        when:
        groovyScript.tagRepository('tagName')
        then:
        1 * getPipelineMock("sh")("git tag -a 'tagName' -m 'Tagged by Jenkins.'")
    }

    def "[githubscm.groovy] pushRemoteTag without credentialsId"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        groovyScript.pushRemoteTag('remote', 'tagName')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'kie-ci', 'usernameVariable': 'GITHUB_USER', 'passwordVariable': 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")('git config --local credential.helper "!f() { echo username=\\user; echo password=\\password; }; f"')
        1 * getPipelineMock("sh")("git push remote --tags tagName")
    }

    def "[githubscm.groovy] pushRemoteTag with credentialsId"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        groovyScript.pushRemoteTag('remote', 'tagName', 'credsId')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'credsId', 'usernameVariable': 'GITHUB_USER', 'passwordVariable': 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")('git config --local credential.helper "!f() { echo username=\\user; echo password=\\password; }; f"')
        1 * getPipelineMock("sh")("git push remote --tags tagName")
    }

    def "[githubscm.groovy] isTagExist ok"() {
        when:
        def output = groovyScript.isTagExist('remote', 'tagName')
        then:
        1 * getPipelineMock("sh")("git fetch remote --tags")
        1 * getPipelineMock("sh")([returnStatus: true, script: "git rev-parse tagName"]) >> 0
        output
    }

    def "[githubscm.groovy] isTagExist ko"() {
        when:
        def output = groovyScript.isTagExist('remote', 'tagName')
        then:
        1 * getPipelineMock("sh")("git fetch remote --tags")
        1 * getPipelineMock("sh")([returnStatus: true, script: "git rev-parse tagName"]) >> 130
        !output
    }

    def "[githubscm.groovy] removeLocalTag"() {
        when:
        groovyScript.removeLocalTag('tagName')
        then:
        1 * getPipelineMock("sh")("git tag -d tagName")
    }

    def "[githubscm.groovy] removeRemoteTag without credentialsId"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        groovyScript.removeRemoteTag('remote', 'tagName')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'kie-ci', 'usernameVariable': 'GITHUB_USER', 'passwordVariable': 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")('git config --local credential.helper "!f() { echo username=\\user; echo password=\\password; }; f"')
        1 * getPipelineMock("sh")("git push --delete remote tagName")
    }

    def "[githubscm.groovy] removeRemoteTag with credentialsId"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        groovyScript.removeRemoteTag('remote', 'tagName', 'credsId')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'credsId', 'usernameVariable': 'GITHUB_USER', 'passwordVariable': 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")('git config --local credential.helper "!f() { echo username=\\user; echo password=\\password; }; f"')
        1 * getPipelineMock("sh")("git push --delete remote tagName")
    }

    def "[githubscm.groovy] tagLocalAndRemoteRepository default"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        groovyScript.tagLocalAndRemoteRepository('remote', 'tagName')
        then:
        1 * getPipelineMock("sh")("git tag -a 'tagName' -m 'Tagged by Jenkins.'")
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'kie-ci', 'usernameVariable': 'GITHUB_USER', 'passwordVariable': 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")('git config --local credential.helper "!f() { echo username=\\user; echo password=\\password; }; f"')
        1 * getPipelineMock("sh")("git push remote --tags tagName")
    }

    def "[githubscm.groovy] tagLocalAndRemoteRepository all params and tag exists"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        groovyScript.tagLocalAndRemoteRepository('remote', 'tagName', 'credsId', 'buildTag', true)
        then:
        1 * getPipelineMock("sh")("git fetch remote --tags")
        1 * getPipelineMock("sh")([returnStatus: true, script: "git rev-parse tagName"]) >> 0
        1 * getPipelineMock("sh")("git tag -d tagName")
        1 * getPipelineMock("sh")("git push --delete remote tagName")
        1 * getPipelineMock("sh")("git tag -a 'tagName' -m 'Tagged by Jenkins in build \"buildTag\".'")
        2 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'credsId', 'usernameVariable': 'GITHUB_USER', 'passwordVariable': 'GITHUB_TOKEN']) >> 'userNamePassword'
        2 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        2 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        2 * getPipelineMock("sh")('git config user.name user')
        2 * getPipelineMock("sh")('git config --local credential.helper "!f() { echo username=\\user; echo password=\\password; }; f"')
        1 * getPipelineMock("sh")("git push remote --tags tagName")
    }

    def "[githubscm.groovy] tagLocalAndRemoteRepository all params and tag not exists"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        groovyScript.tagLocalAndRemoteRepository('remote', 'tagName', 'credsId', 'buildTag', true)
        then:
        1 * getPipelineMock("sh")("git fetch remote --tags")
        1 * getPipelineMock("sh")([returnStatus: true, script: "git rev-parse tagName"]) >> 130
        0 * getPipelineMock("sh")("git tag -d tagName")
        0 * getPipelineMock("sh")("git push --delete remote tagName")
        1 * getPipelineMock("sh")("git tag -a 'tagName' -m 'Tagged by Jenkins in build \"buildTag\".'")
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'credsId', 'usernameVariable': 'GITHUB_USER', 'passwordVariable': 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")('git config --local credential.helper "!f() { echo username=\\user; echo password=\\password; }; f"')
        1 * getPipelineMock("sh")("git push remote --tags tagName")
    }

    def "[githubscm.groovy] createRelease with all params"() {
        setup:
        groovyScript.getBinding().setVariable("GH_USER", 'user')
        groovyScript.getBinding().setVariable("GH_TOKEN", 'password')
        when:
        groovyScript.createRelease('tag','branch','releaseTag','credsId')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'credsId', 'usernameVariable': 'GH_USER', 'passwordVariable': 'GH_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('gh release create tag --target branch --title tag --notes "releaseTag"')
    }

    def "[githubscm.groovy] createRelease without credentialId"() {
        setup:
        groovyScript.getBinding().setVariable("GH_USER", 'user')
        groovyScript.getBinding().setVariable("GH_TOKEN", 'password')
        when:
        groovyScript.createRelease('tag','branch','releaseTag')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'kie-ci', 'usernameVariable': 'GH_USER', 'passwordVariable': 'GH_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('gh release create tag --target branch --title tag --notes "releaseTag"')
    }

    def "[githubscm.groovy] createRelease without credentialId and description"() {
        setup:
        groovyScript.getBinding().setVariable("GH_USER", 'user')
        groovyScript.getBinding().setVariable("GH_TOKEN", 'password')
        when:
        groovyScript.createRelease('tag','branch')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'kie-ci', 'usernameVariable': 'GH_USER', 'passwordVariable': 'GH_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('gh release create tag --target branch --title tag --notes "Release tag"')
    }

    def "[githubscm.groovy] createReleaseWithReleaseNotes with all params"() {
        setup:
        groovyScript.getBinding().setVariable("GH_USER", 'user')
        groovyScript.getBinding().setVariable("GH_TOKEN", 'password')
        when:
        groovyScript.createReleaseWithReleaseNotes('tag','branch','releaseNotes','credsId')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'credsId', 'usernameVariable': 'GH_USER', 'passwordVariable': 'GH_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('gh release create tag --target branch --title tag -F releaseNotes')
    }

    def "[githubscm.groovy] createReleaseWithReleaseNotes without credentialId"() {
        setup:
        groovyScript.getBinding().setVariable("GH_USER", 'user')
        groovyScript.getBinding().setVariable("GH_TOKEN", 'password')
        when:
        groovyScript.createReleaseWithReleaseNotes('tag','branch','releaseNotes')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'kie-ci', 'usernameVariable': 'GH_USER', 'passwordVariable': 'GH_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('gh release create tag --target branch --title tag -F releaseNotes')
    }

    def "[githubscm.groovy] createReleaseWithReleaseNotes without credentialId and without release notes"() {
        setup:
        groovyScript.getBinding().setVariable("GH_USER", 'user')
        groovyScript.getBinding().setVariable("GH_TOKEN", 'password')
        when:
        groovyScript.createReleaseWithReleaseNotes('tag','branch')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'kie-ci', 'usernameVariable': 'GH_USER', 'passwordVariable': 'GH_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('gh release create tag --target branch --title tag -F Release Notes')
    }

    def "[githubscm.groovy] createReleaseWithGeneratedReleaseNotes with all params"() {
        setup:
        groovyScript.getBinding().setVariable("GH_USER", 'user')
        groovyScript.getBinding().setVariable("GH_TOKEN", 'password')
        when:
        groovyScript.createReleaseWithGeneratedReleaseNotes('tag','branch','previoustag','credsId')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'credsId', 'usernameVariable': 'GH_USER', 'passwordVariable': 'GH_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('gh release create tag --target branch --title tag --generate-notes --notes-start-tag previoustag')
    }

    def "[githubscm.groovy] createReleaseWithGeneratedReleaseNotes without credentialId"() {
        setup:
        groovyScript.getBinding().setVariable("GH_USER", 'user')
        groovyScript.getBinding().setVariable("GH_TOKEN", 'password')
        when:
        groovyScript.createReleaseWithGeneratedReleaseNotes('tag','branch','anothertag')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'kie-ci', 'usernameVariable': 'GH_USER', 'passwordVariable': 'GH_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('gh release create tag --target branch --title tag --generate-notes --notes-start-tag anothertag')
    }

    def "[githubscm.groovy] deleteRelease with credentialId"() {
        setup:
        groovyScript.getBinding().setVariable("GH_USER", 'user')
        groovyScript.getBinding().setVariable("GH_TOKEN", 'password')
        when:
        groovyScript.deleteRelease('tag','credsId')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'credsId', 'usernameVariable': 'GH_USER', 'passwordVariable': 'GH_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('gh release delete tag -y')
    }

    def "[githubscm.groovy] deleteRelease without credentialId"() {
        setup:
        groovyScript.getBinding().setVariable("GH_USER", 'user')
        groovyScript.getBinding().setVariable("GH_TOKEN", 'password')
        when:
        groovyScript.deleteRelease('tag')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'kie-ci', 'usernameVariable': 'GH_USER', 'passwordVariable': 'GH_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('gh release delete tag -y')
    }

    def "[githubscm.groovy] isReleaseExist with credentialId"() {
        setup:
        groovyScript.getBinding().setVariable("GH_USER", 'user')
        groovyScript.getBinding().setVariable("GH_TOKEN", 'password')
        when:
        def result = groovyScript.isReleaseExist('tag','credsId')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'credsId', 'usernameVariable': 'GH_USER', 'passwordVariable': 'GH_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")([returnStatus: true, 'script':'gh release view tag']) >> 0
        result == true
    }

    def "[githubscm.groovy] isReleaseExist without credentialId"() {
        setup:
        groovyScript.getBinding().setVariable("GH_USER", 'user')
        groovyScript.getBinding().setVariable("GH_TOKEN", 'password')
        when:
        def result = groovyScript.isReleaseExist('tag')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'kie-ci', 'usernameVariable': 'GH_USER', 'passwordVariable': 'GH_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")([returnStatus: true, 'script':'gh release view tag']) >> 0
        result == true
    }

    def "[githubscm.groovy] pushObject without credentialsId"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        groovyScript.pushObject('remote', 'object')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'kie-ci', 'usernameVariable': 'GITHUB_USER', 'passwordVariable': 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")('git config --local credential.helper "!f() { echo username=\\user; echo password=\\password; }; f"')
        1 * getPipelineMock("sh")("git push remote object")
    }

    def "[githubscm.groovy] pushObject with credentialsId"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        when:
        groovyScript.pushObject('remote', 'object', 'credsId')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'credsId', 'usernameVariable': 'GITHUB_USER', 'passwordVariable': 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")('git config --local credential.helper "!f() { echo username=\\user; echo password=\\password; }; f"')
        1 * getPipelineMock("sh")("git push remote object")
    }

    def "[githubscm.groovy] pushObject exception"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_TOKEN", 'password')
        getPipelineMock("sh")("git push remote object") >> {
            throw new Exception("error when pushing")
        }
        when:
        groovyScript.pushObject('remote', 'object')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'kie-ci', 'usernameVariable': 'GITHUB_USER', 'passwordVariable': 'GITHUB_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('git config user.email user@jenkins.redhat')
        1 * getPipelineMock("sh")('git config user.name user')
        1 * getPipelineMock("sh")('git config --local credential.helper "!f() { echo username=\\user; echo password=\\password; }; f"')
        thrown(Exception)
    }

    def "[githubscm.groovy] getCommit"() {
        when:
        def result = groovyScript.getCommit()
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1']) >> { return 'cd13a88 (HEAD -> BXMSPROD-819, upstream/main, upstream/HEAD, main) [KOGITO-2285] Shared libraries: Git tagging (#41)' }
        result == 'cd13a88 (HEAD -> BXMSPROD-819, upstream/main, upstream/HEAD, main) [KOGITO-2285] Shared libraries: Git tagging (#41)'
    }

    def "[githubscm.groovy] getCommitHash"() {
        when:
        def result = groovyScript.getCommitHash()
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git rev-parse HEAD']) >> { return 'ac36137f12d1bcfa5cdf02b796a1a33d251b48e1' }
        result == 'ac36137f12d1bcfa5cdf02b796a1a33d251b48e1'
    }

    def "[githubscm.groovy] getGitRepositoryURL"() {
        when:
        def result = groovyScript.getGitRepositoryURL()
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> { return 'REPO_URL' }
        result == 'REPO_URL'
    }

    def "[githubscm.groovy] getGitRepositoryName"() {
        when:
        def result = groovyScript.getGitRepositoryName()
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> { return 'REPO_URL' }
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "basename REPO_URL | sed 's|\\.git||g'"]) >> { return 'REPO-NAME' }
        result == 'REPO-NAME'
    }

    def "[githubscm.groovy] getGitRepositoryAuthor"() {
        when:
        def result = groovyScript.getGitRepositoryAuthor()
        then:
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> { return 'REPO_URL' }
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "basename REPO_URL | sed 's|\\.git||g'"]) >> { return 'REPO-NAME' }
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "echo REPO_URL | sed 's|/REPO-NAME.*||g' | sed 's|.*github.com.\\?||g'"]) >> { return 'AUTHOR' }
        result == 'AUTHOR'
    }

    def "[githubscm.groovy] getBranch"() {
        when:
        def result = groovyScript.getBranch()
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git branch --all --contains HEAD']) >> { return '* BXMSPROD-819' }
        result == '* BXMSPROD-819'
    }

    def "[githubscm.groovy] cleanHubAuth"() {
        when:
        groovyScript.cleanHubAuth()
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
    }

    def "[githubscm.groovy] cleanWorkingTree"() {
        when:
        groovyScript.cleanWorkingTree()
        then:
        1 * getPipelineMock("sh")("git clean -xdf")
    }

    def "[githubscm.groovy] getRemoteInfo"() {
        when:
        def result = groovyScript.getRemoteInfo('remoteName', 'configName')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.remoteName.configName']) >> { return '+refs/heads/*:refs/remotes/origin/*' }
        result == '+refs/heads/*:refs/remotes/origin/*'
    }

    def "[githubscm.groovy] hasForkPullRequest with pull requests"() {
        when:
        def result = groovyScript.hasForkPullRequest('group', 'repository', 'author', 'branch')
        then:
        1 * getPipelineMock("string.call")(['credentialsId': 'kie-ci1-token', 'variable': 'OAUTHTOKEN'])
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl --globoff -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=author:branch&state=open'"]) >> pullRequestInfo
        result
    }

    def "[githubscm.groovy] hasForkPullRequest with pull requests different credentials"() {
        when:
        def result = groovyScript.hasForkPullRequest('group', 'repository', 'author', 'branch', 'credentials2')
        then:
        1 * getPipelineMock("string.call")(['credentialsId': 'credentials2', 'variable': 'OAUTHTOKEN'])
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl --globoff -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=author:branch&state=open'"]) >> pullRequestInfo
        result
    }

    def "[githubscm.groovy] hasForkPullRequest without pull requests"() {
        when:
        def result = groovyScript.hasForkPullRequest('group', 'repository', 'author', 'branch')
        then:
        1 * getPipelineMock("string.call")(['credentialsId': 'kie-ci1-token', 'variable': 'OAUTHTOKEN'])
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl --globoff -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=author:branch&state=open'"]) >> pullRequestInfoEmpty
        !result
    }

    def "[githubscm.groovy] hasOriginPullRequest with pull requests"() {
        when:
        def result = groovyScript.hasOriginPullRequest('group', 'repository', 'branch')
        then:
        1 * getPipelineMock("string.call")(['credentialsId': 'kie-ci1-token', 'variable': 'OAUTHTOKEN'])
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl --globoff -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=group:branch&state=open'"]) >> pullRequestInfo
        result
    }

    def "[githubscm.groovy] hasOriginPullRequest without pull requests"() {
        when:
        def result = groovyScript.hasOriginPullRequest('group', 'repository', 'branch')
        then:
        1 * getPipelineMock("string.call")(['credentialsId': 'kie-ci1-token', 'variable': 'OAUTHTOKEN'])
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl --globoff -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=group:branch&state=open'"]) >> pullRequestInfoEmpty
        !result
    }

    def "[githubscm.groovy] hasPullRequest with fork PR"() {
        when:
        def result = groovyScript.hasPullRequest('group', 'repository', 'author', 'branch')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl --globoff -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=author:branch&state=open'"]) >> pullRequestInfo
        0 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl --globoff -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=group:branch&state=open'"])
        result
    }

    def "[githubscm.groovy] hasPullRequest with origin PR"() {
        when:
        def result = groovyScript.hasPullRequest('group', 'repository', 'author', 'branch')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl --globoff -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=author:branch&state=open'"]) >> pullRequestInfoEmpty
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl --globoff -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=group:branch&state=open'"]) >> pullRequestInfo
        result
    }

    def "[githubscm.groovy] hasPullRequest without PR"() {
        when:
        def result = groovyScript.hasPullRequest('group', 'repository', 'author', 'branch')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl --globoff -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=author:branch&state=open'"]) >> pullRequestInfoEmpty
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl --globoff -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=group:branch&state=open'"]) >> pullRequestInfoEmpty
        !result
    }

    def "[githubscm.groovy] getForkedProject exists"() {
        when:
        def result = groovyScript.getForkedProjectName('groupx', 'repox', 'irtyamine')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks?per_page=100&page=1'"]) >> forkListInfo
        0 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks?per_page=100&page=2'"])
        'github-action-build-chain' == result
    }

    def "[githubscm.groovy] getForkedProject no existing"() {
        when:
        def result = groovyScript.getForkedProjectName('groupx', 'repox', 'unknownuser')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks?per_page=100&page=1'"]) >> forkListInfo
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks?per_page=100&page=2'"]) >> forkListInfoEmpty
        0 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks?per_page=100&page=3'"])
        null == result
    }

    def "[githubscm.groovy] getForkedProject empty"() {
        when:
        def result = groovyScript.getForkedProjectName('groupx', 'repox', 'irtyamine')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks?per_page=100&page=1'"]) >> forkListInfoEmpty
        0 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks?per_page=100&page=2'"])
        null == result
    }

    def "[githubscm.groovy] getForkedProject pagination"() {
        when:
        def result = groovyScript.getForkedProjectName('groupx', 'repox', 'LeonidLapshin')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks?per_page=100&page=1'"]) >> forkListInfo
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks?per_page=100&page=2'"]) >> forkListInfo
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks?per_page=100&page=3'"]) >> forkListInfoPage3
        0 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks?per_page=100&page=4'"])
        'appformer' == result
    }

    def "[githubscm.groovy] getForkedProject pagination not present"() {
        when:
        def result = groovyScript.getForkedProjectName('groupx', 'repox', 'notexistinguser')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks?per_page=100&page=1'"]) >> forkListInfo
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks?per_page=100&page=2'"]) >> forkListInfoEmpty
        0 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks?per_page=100&page=3'"])
        null == result
    }

    def "[githubscm.groovy] getForkedProject same group and owner"() {
        when:
        def result = groovyScript.getForkedProjectName('groupx', 'repox', 'groupx')
        then:
        0 * getPipelineMock("sh")(_)
        'repox' == result
    }

    def "[githubscm.groovy] getForkedProject missing property exception"() {
        when:
        groovyScript.getForkedProjectName('groupx', 'repox', 'irtyamine')
        then:
        3 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks?per_page=100&page=1'"]) >> forkListInfoMissingOwner
        thrown(Exception)
    }

    def "[githubscm.groovy] getForkedProject missing property 2 replays"() {
        when:
        def result = groovyScript.getForkedProjectName('groupx', 'repox', 'irtyamine')
        then:
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks?per_page=100&page=1'"]) >> forkListInfoMissingOwner
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks?per_page=100&page=1'"]) >> forkListInfo
        'github-action-build-chain' == result
    }

    def "[githubscm.groovy] isThereAnyChanges no change"() {
        when:
        def result = groovyScript.isThereAnyChanges()
        then:
        1 * getPipelineMock("sh")(['script': 'git status --porcelain', 'returnStdout': true]) >> ''
    }

    def "[githubscm.groovy] isThereAnyChanges with changes"() {
        when:
        def result = groovyScript.isThereAnyChanges()
        then:
        1 * getPipelineMock("sh")(['script': 'git status --porcelain', 'returnStdout': true]) >> 'anything'
    }

    def "[githubscm.groovy] updateReleaseBody with all params"() {
        setup:
        groovyScript.getBinding().setVariable("GH_USER", 'user')
        groovyScript.getBinding().setVariable("GH_TOKEN", 'password')
        when:
        groovyScript.updateReleaseBody('tag','credsId')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'credsId', 'usernameVariable': 'GH_USER', 'passwordVariable': 'GH_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('gh release edit tag -F release_notes')
    }

    def "[githubscm.groovy] updateReleaseBody without credentialId"() {
        setup:
        groovyScript.getBinding().setVariable("GH_USER", 'user')
        groovyScript.getBinding().setVariable("GH_TOKEN", 'password')
        when:
        groovyScript.updateReleaseBody('tag')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'kie-ci', 'usernameVariable': 'GH_USER', 'passwordVariable': 'GH_TOKEN']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('gh release edit tag -F release_notes')
    }

    def "[githubscm.groovy] getPreviousTag when tag does not exist"() {
        when:
        def result = groovyScript.getPreviousTag('tag')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git tag --sort=-taggerdate | head -n 1']) >> { return '1.39.0.Final' }
        result == '1.39.0.Final'
    }

    def "[githubscm.groovy] getPreviousTag when tag exists"() {
        when:
        def result = groovyScript.getPreviousTag('tag')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git tag --sort=-taggerdate | head -n 1']) >> 'tag'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git tag --sort=-taggerdate | head -n 2 | tail -n 1']) >> { return '1.39.0.Final' }
        result == '1.39.0.Final'
    }

    def "[githubscm.groovy] getLatestTag default"() {
        when:
        def result = groovyScript.getLatestTag()
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "git tag --sort=-taggerdate | head -n 1"]) >> 'TAG'
        result == 'TAG'
    }

    def "[githubscm.groovy] getLatestTag with startsWith"() {
        when:
        def result = groovyScript.getLatestTag('START')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "git tag --sort=-taggerdate | grep '^START' | head -n 1"]) >> 'TAG '
        result == 'TAG'
    }

    def "[githubscm.groovy] getLatestTag with endsWith"() {
        when:
        def result = groovyScript.getLatestTag('', 'END')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "git tag --sort=-taggerdate | grep 'END\$' | head -n 1"]) >> 'TAG '
        result == 'TAG'
    }

    def "[githubscm.groovy] getLatestTag with ignoreTags"() {
        when:
        def result = groovyScript.getLatestTag('', '', [ 'TAG1', 'TAG2' ])
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "git tag --sort=-taggerdate | grep -v 'TAG1' | grep -v 'TAG2' | head -n 1"]) >> 'TAG '
        result == 'TAG'
    }

    def "[githubscm.groovy] getLatestTag with all params"() {
        when:
        def result = groovyScript.getLatestTag('START', 'END', [ 'TAG1', 'TAG2' ])
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "git tag --sort=-taggerdate | grep -v 'TAG1' | grep -v 'TAG2' | grep '^START' | grep 'END\$' | head -n 1"]) >> 'TAG '
        result == 'TAG'
    }

    def "[githubscm.groovy] prepareCommitStatusInformation default"() {
        setup:
        def env = [:]
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.prepareCommitStatusInformation('REPO-NAME', 'AUTHOR', 'BRANCH')
        then:
        1 * getPipelineMock("util.generateTempFolder")() >> 'TEMP_FOLDER'
        1 * getPipelineMock("dir")('TEMP_FOLDER', _)
        1 * getPipelineMock("checkout")([$class: "GitSCM", branches: [[name: "BRANCH"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/AUTHOR/REPO-NAME.git"]]])
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> 'REPO_URL'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git rev-parse HEAD']) >> 'COMMIT_SHA '
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_REPO_URL'] == "REPO_URL"
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_SHA'] == "COMMIT_SHA"
    }

    def "[githubscm.groovy] prepareCommitStatusInformation with credentials"() {
        setup:
        def env = [:]
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.prepareCommitStatusInformation('REPO-NAME', 'AUTHOR', 'BRANCH', 'CREDS_ID')
        then:
        1 * getPipelineMock("util.generateTempFolder")() >> 'TEMP_FOLDER'
        1 * getPipelineMock("dir")('TEMP_FOLDER', _)
        1 * getPipelineMock("checkout")([$class: "GitSCM", branches: [[name: "BRANCH"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "CREDS_ID", "url": "https://github.com/AUTHOR/REPO-NAME.git"]]])
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> 'REPO_URL'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git rev-parse HEAD']) >> 'COMMIT_SHA '
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_REPO_URL'] == "REPO_URL"
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_SHA'] == "COMMIT_SHA"
    }
    
    def "[githubscm.groovy] prepareCommitStatusInformationForPullRequest default"() {
        setup:
        def env = [:]
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.prepareCommitStatusInformationForPullRequest('REPO-NAME', 'AUTHOR', 'BRANCH', 'TARGET_AUTHOR')
        then:
        1 * getPipelineMock("util.generateTempFolder")() >> 'TEMP_FOLDER'
        1 * getPipelineMock("dir")('TEMP_FOLDER', _)
        1 * getPipelineMock("checkout")([$class: "GitSCM", branches: [[name: "BRANCH"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/AUTHOR/REPO-NAME.git"]]])
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> 'REPO_URL'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git rev-parse HEAD']) >> 'COMMIT_SHA '
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_REPO_URL'] == "https://github.com/TARGET_AUTHOR/REPO-NAME"
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_SHA'] == "COMMIT_SHA"
    }

    def "[githubscm.groovy] prepareCommitStatusInformationForPullRequest with credentials"() {
        setup:
        def env = [:]
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.prepareCommitStatusInformationForPullRequest('REPO-NAME', 'AUTHOR', 'BRANCH', 'TARGET_AUTHOR', 'CREDS_ID')
        then:
        1 * getPipelineMock("util.generateTempFolder")() >> 'TEMP_FOLDER'
        1 * getPipelineMock("dir")('TEMP_FOLDER', _)
        1 * getPipelineMock("checkout")([$class: "GitSCM", branches: [[name: "BRANCH"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "CREDS_ID", "url": "https://github.com/AUTHOR/REPO-NAME.git"]]])
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> 'REPO_URL'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git rev-parse HEAD']) >> 'COMMIT_SHA '
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_REPO_URL'] == "https://github.com/TARGET_AUTHOR/REPO-NAME"
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_SHA'] == "COMMIT_SHA"
    }

    def "[githubscm.groovy] setCommitStatusRepoURLEnv default"() {
        setup:
        def env = [:]
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.setCommitStatusRepoURLEnv('repo-with-dash')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> 'REPO_URL'
        groovyScript.getBinding().getVariable("env")['REPO-WITH-DASH_COMMIT_STATUS_REPO_URL'] == "REPO_URL"
    }

    def "[githubscm.groovy] setCommitStatusRepoURLEnv with given url"() {
        setup:
        def env = [:]
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.setCommitStatusRepoURLEnv('repo-with-dash', 'URL')
        then:
        0 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> 'REPO_URL'
        groovyScript.getBinding().getVariable("env")['REPO-WITH-DASH_COMMIT_STATUS_REPO_URL'] == "URL"
    }

    def "[githubscm.groovy] setCommitStatusShaEnv default"() {
        setup:
        def env = [:]
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.setCommitStatusShaEnv('repo-with-dash')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git rev-parse HEAD']) >> 'COMMIT_SHA '
        groovyScript.getBinding().getVariable("env")['REPO-WITH-DASH_COMMIT_STATUS_SHA'] == "COMMIT_SHA"
    }

    def "[githubscm.groovy] setCommitStatusShaEnv with given url"() {
        setup:
        def env = [:]
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.setCommitStatusShaEnv('repo-with-dash', 'SHA')
        then:
        0 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> 'REPO_URL'
        groovyScript.getBinding().getVariable("env")['REPO-WITH-DASH_COMMIT_STATUS_SHA'] == "SHA"
    }

    def "[githubscm.groovy] updateGithubCommitStatus default, no env"() {
        setup:
        def env = [:]
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.updateGithubCommitStatus('CHECK_NAME', 'STATE', 'MESSAGE')
        then:
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> 'REPO_URL'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "basename REPO_URL | sed 's|\\.git||g'" ]) >> 'REPO-NAME'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git rev-parse HEAD']) >> 'COMMIT_SHA '
        1 * getPipelineMock("step")([
            $class: 'GitHubCommitStatusSetter',
            commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: 'COMMIT_SHA'],
            contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'CHECK_NAME'],
            reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'REPO_URL'],
            statusResultSource: [ $class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: 'MESSAGE', state: 'STATE']] ],
        ])
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_REPO_URL'] == "REPO_URL"
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_SHA'] == "COMMIT_SHA"
    }

    def "[githubscm.groovy] updateGithubCommitStatus default, with full env"() {
        setup:
        def env = [:]
        env."REPO-NAME_COMMIT_STATUS_REPO_URL" = 'PREVIOUS_REPO_URL'
        env."REPO-NAME_COMMIT_STATUS_SHA" = 'PREVIOUS_COMMIT_SHA'
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.updateGithubCommitStatus('CHECK_NAME', 'STATE', 'MESSAGE')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> 'REPO_URL'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "basename REPO_URL | sed 's|\\.git||g'" ]) >> 'REPO-NAME'
        0 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git rev-parse HEAD']) >> 'COMMIT_SHA '
        1 * getPipelineMock("step")([
            $class: 'GitHubCommitStatusSetter',
            commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: 'PREVIOUS_COMMIT_SHA'],
            contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'CHECK_NAME'],
            reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'PREVIOUS_REPO_URL'],
            statusResultSource: [ $class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: 'MESSAGE', state: 'STATE']] ],
        ])
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_REPO_URL'] == "PREVIOUS_REPO_URL"
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_SHA'] == "PREVIOUS_COMMIT_SHA"
    }

    def "[githubscm.groovy] updateGithubCommitStatus default, with only COMMIT_STATUS_REPO_URL env defined"() {
        setup:
        def env = [:]
        env."REPO-NAME_COMMIT_STATUS_REPO_URL" = 'PREVIOUS_REPO_URL'
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.updateGithubCommitStatus('CHECK_NAME', 'STATE', 'MESSAGE')
        then:
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> 'REPO_URL'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "basename REPO_URL | sed 's|\\.git||g'" ]) >> 'REPO-NAME'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git rev-parse HEAD']) >> 'COMMIT_SHA '
        1 * getPipelineMock("step")([
            $class: 'GitHubCommitStatusSetter',
            commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: 'COMMIT_SHA'],
            contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'CHECK_NAME'],
            reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'REPO_URL'],
            statusResultSource: [ $class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: 'MESSAGE', state: 'STATE']] ],
        ])
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_REPO_URL'] == "REPO_URL"
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_SHA'] == "COMMIT_SHA"
    }

    def "[githubscm.groovy] updateGithubCommitStatus default, with only COMMIT_STATUS_SHA env defined"() {
        setup:
        def env = [:]
        env."REPO-NAME_COMMIT_STATUS_SHA" = 'PREVIOUS_COMMIT_SHA'
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.updateGithubCommitStatus('CHECK_NAME', 'STATE', 'MESSAGE')
        then:
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> 'REPO_URL'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "basename REPO_URL | sed 's|\\.git||g'" ]) >> 'REPO-NAME'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git rev-parse HEAD']) >> 'COMMIT_SHA '
        1 * getPipelineMock("step")([
            $class: 'GitHubCommitStatusSetter',
            commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: 'COMMIT_SHA'],
            contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'CHECK_NAME'],
            reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'REPO_URL'],
            statusResultSource: [ $class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: 'MESSAGE', state: 'STATE']] ],
        ])
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_REPO_URL'] == "REPO_URL"
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_SHA'] == "COMMIT_SHA"
    }

    def "[githubscm.groovy] updateGithubCommitStatus default, with repository"() {
        setup:
        def env = [:]
        groovyScript.getBinding().setVariable("env", env)
        when:
        groovyScript.updateGithubCommitStatus('CHECK_NAME', 'STATE', 'MESSAGE', 'repository')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> 'REPO_URL'
        0 * getPipelineMock("sh")(['returnStdout': true, 'script': "basename REPO_URL | sed 's|\\.git||g'" ]) >> 'REPO-NAME'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git rev-parse HEAD']) >> 'COMMIT_SHA '
        1 * getPipelineMock("step")([
            $class: 'GitHubCommitStatusSetter',
            commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: 'COMMIT_SHA'],
            contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'CHECK_NAME'],
            reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'REPO_URL'],
            statusResultSource: [ $class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: 'MESSAGE', state: 'STATE']] ],
        ])
        groovyScript.getBinding().getVariable("env")['REPOSITORY_COMMIT_STATUS_REPO_URL'] == "REPO_URL"
        groovyScript.getBinding().getVariable("env")['REPOSITORY_COMMIT_STATUS_SHA'] == "COMMIT_SHA"
    }

    def "[githubscm.groovy] updateGithubCommitStatusFromBuildResult and result SUCCESS"() {
        setup:
        groovyScript.getBinding().setVariable("env", [:])
        groovyScript.getBinding().setVariable("currentBuild", [ currentResult: 'SUCCESS' ])
        when:
        groovyScript.updateGithubCommitStatusFromBuildResult('CHECK_NAME')
        then:
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> 'REPO_URL'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "basename REPO_URL | sed 's|\\.git||g'" ]) >> 'REPO-NAME'
        1 * getPipelineMock("util.retrieveTestResults")() >> [passCount:10, skipCount: 3, failCount: 2]
        1 * getPipelineMock("util.getJobDurationInSeconds")() >> 3824
        1 * getPipelineMock("util.displayDurationFromSeconds")(3824) >> '1h2m4s'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git rev-parse HEAD']) >> 'COMMIT_SHA '
        1 * getPipelineMock("step")([
            $class: 'GitHubCommitStatusSetter',
            commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: 'COMMIT_SHA'],
            contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'CHECK_NAME'],
            reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'REPO_URL'],
            statusResultSource: [ $class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: "(1h2m4s) Check is successful. 15 tests run, 2 failed, 3 skipped.", state: 'SUCCESS']] ],
        ])
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_REPO_URL'] == "REPO_URL"
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_SHA'] == "COMMIT_SHA"
    }

    def "[githubscm.groovy] updateGithubCommitStatusFromBuildResult and result UNSTABLE"() {
        setup:
        groovyScript.getBinding().setVariable("env", [:])
        groovyScript.getBinding().setVariable("currentBuild", [ currentResult: 'UNSTABLE' ])
        when:
        groovyScript.updateGithubCommitStatusFromBuildResult('CHECK_NAME')
        then:
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> 'REPO_URL'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "basename REPO_URL | sed 's|\\.git||g'" ]) >> 'REPO-NAME'
        1 * getPipelineMock("util.retrieveTestResults")() >> [passCount:10, skipCount: 3, failCount: 2]
        1 * getPipelineMock("util.getJobDurationInSeconds")() >> 3824
        1 * getPipelineMock("util.displayDurationFromSeconds")(3824) >> '1h2m4s'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git rev-parse HEAD']) >> 'COMMIT_SHA '
        1 * getPipelineMock("step")([
            $class: 'GitHubCommitStatusSetter',
            commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: 'COMMIT_SHA'],
            contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'CHECK_NAME'],
            reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'REPO_URL'],
            statusResultSource: [ $class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: "(1h2m4s) Test failures occurred. 15 tests run, 2 failed, 3 skipped.", state: 'FAILURE']] ],
        ])
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_REPO_URL'] == "REPO_URL"
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_SHA'] == "COMMIT_SHA"
    }

    def "[githubscm.groovy] updateGithubCommitStatusFromBuildResult and result ABORTED"() {
        setup:
        groovyScript.getBinding().setVariable("env", [:])
        groovyScript.getBinding().setVariable("currentBuild", [ currentResult: 'ABORTED' ])
        when:
        groovyScript.updateGithubCommitStatusFromBuildResult('CHECK_NAME')
        then:
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> 'REPO_URL'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "basename REPO_URL | sed 's|\\.git||g'" ]) >> 'REPO-NAME'
        1 * getPipelineMock("util.retrieveTestResults")() >> [passCount:10, skipCount: 3, failCount: 2]
        1 * getPipelineMock("util.getJobDurationInSeconds")() >> 3824
        1 * getPipelineMock("util.displayDurationFromSeconds")(3824) >> '1h2m4s'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git rev-parse HEAD']) >> 'COMMIT_SHA '
        1 * getPipelineMock("step")([
            $class: 'GitHubCommitStatusSetter',
            commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: 'COMMIT_SHA'],
            contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'CHECK_NAME'],
            reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'REPO_URL'],
            statusResultSource: [ $class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: "(1h2m4s) Job aborted. 15 tests run, 2 failed, 3 skipped.", state: 'ERROR']] ],
        ])
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_REPO_URL'] == "REPO_URL"
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_SHA'] == "COMMIT_SHA"
    }

    def "[githubscm.groovy] updateGithubCommitStatusFromBuildResult and result FAILURE"() {
        setup:
        groovyScript.getBinding().setVariable("env", [:])
        groovyScript.getBinding().setVariable("currentBuild", [ currentResult: 'FAILURE' ])
        when:
        groovyScript.updateGithubCommitStatusFromBuildResult('CHECK_NAME')
        then:
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> 'REPO_URL'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "basename REPO_URL | sed 's|\\.git||g'" ]) >> 'REPO-NAME'
        1 * getPipelineMock("util.retrieveTestResults")() >> [passCount:10, skipCount: 3, failCount: 2]
        1 * getPipelineMock("util.getJobDurationInSeconds")() >> 3824
        1 * getPipelineMock("util.displayDurationFromSeconds")(3824) >> '1h2m4s'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git rev-parse HEAD']) >> 'COMMIT_SHA '
        1 * getPipelineMock("step")([
            $class: 'GitHubCommitStatusSetter',
            commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: 'COMMIT_SHA'],
            contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'CHECK_NAME'],
            reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'REPO_URL'],
            statusResultSource: [ $class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: "(1h2m4s) Issue in pipeline. 15 tests run, 2 failed, 3 skipped.", state: 'ERROR']] ],
        ])
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_REPO_URL'] == "REPO_URL"
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_SHA'] == "COMMIT_SHA"
    }

    def "[githubscm.groovy] updateGithubCommitStatusFromBuildResult and any other result"() {
        setup:
        groovyScript.getBinding().setVariable("env", [:])
        groovyScript.getBinding().setVariable("currentBuild", [ currentResult: 'anyother' ])
        when:
        groovyScript.updateGithubCommitStatusFromBuildResult('CHECK_NAME')
        then:
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> 'REPO_URL'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "basename REPO_URL | sed 's|\\.git||g'" ]) >> 'REPO-NAME'
        1 * getPipelineMock("util.retrieveTestResults")() >> [passCount:10, skipCount: 3, failCount: 2]
        1 * getPipelineMock("util.getJobDurationInSeconds")() >> 3824
        1 * getPipelineMock("util.displayDurationFromSeconds")(3824) >> '1h2m4s'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git rev-parse HEAD']) >> 'COMMIT_SHA '
        1 * getPipelineMock("step")([
            $class: 'GitHubCommitStatusSetter',
            commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: 'COMMIT_SHA'],
            contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'CHECK_NAME'],
            reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'REPO_URL'],
            statusResultSource: [ $class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: "(1h2m4s) Issue in pipeline. 15 tests run, 2 failed, 3 skipped.", state: 'ERROR']] ],
        ])
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_REPO_URL'] == "REPO_URL"
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_SHA'] == "COMMIT_SHA"
    }

    def "[githubscm.groovy] updateGithubCommitStatusFromBuildResult without test results"() {
        setup:
        groovyScript.getBinding().setVariable("env", [:])
        groovyScript.getBinding().setVariable("currentBuild", [ currentResult: 'SUCCESS' ])
        when:
        groovyScript.updateGithubCommitStatusFromBuildResult('CHECK_NAME')
        then:
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git config --get remote.origin.url | head -n 1']) >> 'REPO_URL'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "basename REPO_URL | sed 's|\\.git||g'" ]) >> 'REPO-NAME'
        1 * getPipelineMock("util.retrieveTestResults")()
        1 * getPipelineMock("util.getJobDurationInSeconds")() >> 3824
        1 * getPipelineMock("util.displayDurationFromSeconds")(3824) >> '1h2m4s'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git rev-parse HEAD']) >> 'COMMIT_SHA '
        1 * getPipelineMock("step")([
            $class: 'GitHubCommitStatusSetter',
            commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: 'COMMIT_SHA'],
            contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'CHECK_NAME'],
            reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'REPO_URL'],
            statusResultSource: [ $class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: "(1h2m4s) Check is successful. No test results found.", state: 'SUCCESS']] ],
        ])
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_REPO_URL'] == "REPO_URL"
        groovyScript.getBinding().getVariable("env")['REPO-NAME_COMMIT_STATUS_SHA'] == "COMMIT_SHA"
    }
}
