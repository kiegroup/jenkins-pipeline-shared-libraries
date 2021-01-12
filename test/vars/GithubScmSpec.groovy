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
        groovyScript.checkoutIfExists('repository', 'author', 'branches', 'defaultAuthor', 'master')
        then:
        2 * getPipelineMock("checkout")(gitSCM)
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/pulls?head=author:branches&state=open'"]) >> pullRequestInfo
    }

    def "[githubscm.groovy] checkoutIfExists first repo does not exist"() {
        setup:
        def gitSCM = [$class: "GitSCM", branches: [[name: "branches"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/author/repository.git"]]]
        when:
        groovyScript.checkoutIfExists('repository', 'author', 'branches', 'defaultAuthor', 'master')
        then:
        0 * getPipelineMock("checkout")(null)

        2 * getPipelineMock("checkout")(gitSCM)
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/pulls?head=author:branches&state=open'"]) >> pullRequestInfo
    }

    def "[githubscm.groovy] checkoutIfExists with merge true"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        def repositoryScmInformation = [$class: "GitSCM", branches: [[name: "branches"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/author/repository.git"]]]
        def repositoryScmInformationMaster = [$class: "GitSCM", branches: [[name: "master"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/defaultAuthor/repository.git"]]]
        when:
        groovyScript.checkoutIfExists('repository', 'author', 'branches', 'defaultAuthor', 'master', true)
        then:
        1 * getPipelineMock('checkout')(repositoryScmInformation)
        1 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'kie-ci', variable: 'kieCiUserPassword']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock('checkout')(repositoryScmInformationMaster)
        1 * getPipelineMock('sh')('git pull https://user:password@github.com/author/repository branches')
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1']) >> 'git commit information'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/pulls?head=author:branches&state=open'"]) >> pullRequestInfo
    }

    def "[githubscm.groovy] checkoutIfExists with merge true and different forked project name"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        def repositoryScmInformation = [$class: "GitSCM", branches: [[name: "branches"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/author/irtyamine.git"]]]
        def repositoryScmInformationMaster = [$class: "GitSCM", branches: [[name: "master"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/defaultAuthor/repository.git"]]]
        when:
        groovyScript.checkoutIfExists('repository', 'irtyamine', 'branches', 'defaultAuthor', 'master', true)
        then:
        0 * getPipelineMock('checkout')(repositoryScmInformation)
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/forks?per_page=100&page=1'"]) >> forkListInfo
        1 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'kie-ci', variable: 'kieCiUserPassword']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock('checkout')(repositoryScmInformationMaster)
        1 * getPipelineMock('sh')('git pull https://user:password@github.com/irtyamine/github-action-build-chain branches')
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1']) >> 'git commit information'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/pulls?head=irtyamine:branches&state=open'"]) >> pullRequestInfo
    }

    def "[githubscm.groovy] checkoutIfExists with merge true and different credentials"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        def repositoryScmInformation = [$class: "GitSCM", branches: [[name: "branches"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "ci-usernamePassword", "url": "https://github.com/author/irtyamine.git"]]]
        def repositoryScmInformationMaster = [$class: "GitSCM", branches: [[name: "master"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "ci-usernamePassword", "url": "https://github.com/defaultAuthor/repository.git"]]]
        when:
        groovyScript.checkoutIfExists('repository', 'author', 'branches', 'defaultAuthor', 'master', true, ['token': 'ci-token', 'usernamePassword': 'ci-usernamePassword'])
        then:
        0 * getPipelineMock('checkout')(repositoryScmInformation)
        1 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'ci-usernamePassword', variable: 'kieCiUserPassword']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock('checkout')(repositoryScmInformationMaster)
        1 * getPipelineMock('sh')('git pull https://user:password@github.com/author/repository branches')
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1']) >> 'git commit information'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/pulls?head=author:branches&state=open'"]) >> pullRequestInfo
        2 * getPipelineMock("string.call")(['credentialsId': 'ci-token', 'variable': 'OAUTHTOKEN'])
    }

    def "[githubscm.groovy] checkoutIfExists has not PR"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        def repositoryScmInformation = [$class: "GitSCM", branches: [[name: "branches"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/author/irtyamine.git"]]]
        def repositoryScmInformationMaster = [$class: "GitSCM", branches: [[name: "master"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/defaultAuthor/repository.git"]]]
        when:
        groovyScript.checkoutIfExists('repository', 'author', 'branches', 'defaultAuthor', 'master', true)
        then:
        0 * getPipelineMock('checkout')(repositoryScmInformation)
        0 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'kie-ci', variable: 'kieCiUserPassword']) >> 'userNamePassword'

        1 * getPipelineMock('checkout')(repositoryScmInformationMaster)
        0 * getPipelineMock('sh')('git pull https://user:password@github.com/author/repository branches')

        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/pulls?head=author:branches&state=open'"]) >> pullRequestInfoEmpty
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/pulls?head=defaultAuthor:branches&state=open'"]) >> pullRequestInfoEmpty
    }

    def "[githubscm.groovy] checkoutIfExists Multibranch pipeline job"() {
        setup:
        def gitSCM = [$class: "GitSCM", branches: [[name: "master"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: "CleanBeforeCheckout"], [$class: "SubmoduleOption", disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: "", trackingSubmodules: false], [$class: "RelativeTargetDirectory", relativeTargetDir: "./"]], "submoduleCfg": [], "userRemoteConfigs": [["credentialsId": "kie-ci", "url": "https://github.com/kiegroup/repository.git"]]]
        when:
        groovyScript.checkoutIfExists('repository', 'kiegroup', 'master', 'kiegroup', 'master')
        then:
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/kiegroup/repository/pulls?head=kiegroup:master&state=open'"]) >> pullRequestInfoEmpty
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

    def "[githubscm.groovy] commitChanges without files to add"() {
        when:
        groovyScript.commitChanges('commit message')
        then:
        1 * getPipelineMock("sh")('git add --all')
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
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hub pull-request -m 'PR Title' -m '' -b 'master'"]) >> 'shResult'
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
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hub pull-request -m 'PR Title' -m 'PR body.' -b 'master'"]) >> 'shResult'
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
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hub pull-request -m 'PR Title' -m '' -b 'master'"]) >> { throw new Exception('error') }
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
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1']) >> { return 'cd13a88 (HEAD -> BXMSPROD-819, upstream/master, upstream/HEAD, master) [KOGITO-2285] Shared libraries: Git tagging (#41)' }
        result == 'cd13a88 (HEAD -> BXMSPROD-819, upstream/master, upstream/HEAD, master) [KOGITO-2285] Shared libraries: Git tagging (#41)'
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
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=author:branch&state=open'"]) >> pullRequestInfo
        result
    }

    def "[githubscm.groovy] hasForkPullRequest with pull requests different credentials"() {
        when:
        def result = groovyScript.hasForkPullRequest('group', 'repository', 'author', 'branch', 'credentials2')
        then:
        1 * getPipelineMock("string.call")(['credentialsId': 'credentials2', 'variable': 'OAUTHTOKEN'])
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=author:branch&state=open'"]) >> pullRequestInfo
        result
    }

    def "[githubscm.groovy] hasForkPullRequest without pull requests"() {
        when:
        def result = groovyScript.hasForkPullRequest('group', 'repository', 'author', 'branch')
        then:
        1 * getPipelineMock("string.call")(['credentialsId': 'kie-ci1-token', 'variable': 'OAUTHTOKEN'])
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=author:branch&state=open'"]) >> pullRequestInfoEmpty
        !result
    }

    def "[githubscm.groovy] hasOriginPullRequest with pull requests"() {
        when:
        def result = groovyScript.hasOriginPullRequest('group', 'repository', 'branch')
        then:
        1 * getPipelineMock("string.call")(['credentialsId': 'kie-ci1-token', 'variable': 'OAUTHTOKEN'])
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=group:branch&state=open'"]) >> pullRequestInfo
        result
    }

    def "[githubscm.groovy] hasOriginPullRequest without pull requests"() {
        when:
        def result = groovyScript.hasOriginPullRequest('group', 'repository', 'branch')
        then:
        1 * getPipelineMock("string.call")(['credentialsId': 'kie-ci1-token', 'variable': 'OAUTHTOKEN'])
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=group:branch&state=open'"]) >> pullRequestInfoEmpty
        !result
    }

    def "[githubscm.groovy] hasPullRequest with fork PR"() {
        when:
        def result = groovyScript.hasPullRequest('group', 'repository', 'author', 'branch')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=author:branch&state=open'"]) >> pullRequestInfo
        0 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=group:branch&state=open'"])
        result
    }

    def "[githubscm.groovy] hasPullRequest with origin PR"() {
        when:
        def result = groovyScript.hasPullRequest('group', 'repository', 'author', 'branch')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=author:branch&state=open'"]) >> pullRequestInfoEmpty
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=group:branch&state=open'"]) >> pullRequestInfo
        result
    }

    def "[githubscm.groovy] hasPullRequest without PR"() {
        when:
        def result = groovyScript.hasPullRequest('group', 'repository', 'author', 'branch')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=author:branch&state=open'"]) >> pullRequestInfoEmpty
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/group/repository/pulls?head=group:branch&state=open'"]) >> pullRequestInfoEmpty
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

    def "[githubscm.groovy] set default base branch"() {
        when:
        def result = groovyScript.setDefaultBranch('repo', 'defaultBranch', 'credentialId', 'author')
        then:
        1 * getPipelineMock("sh")(['script':"../gh api -XPATCH 'repos/author/repo' -f default_branch=defaultBranch | jq '.default_branch'", 'returnStdout':true]) >> "defaultBranch"
        1 * getPipelineMock( "echo" )("[INFO] author/repo's default branch has been updated to defaultBranch.")
    }

    def "[githubscm.groovy] force push protected branch"() {
        when:
        def result = groovyScript.forcePushProtectedBranch('repo', 'defaultBranch', 'tempBranch', 'credentialId', 'author')
        then:
        1 * getPipelineMock("sh")("git config --local credential.helper '!f() { echo username=Mock Generator for [GITHUB_USER]; echo password=Mock Generator for [GITHUB_TOKEN]; }; f'")
        1 * getPipelineMock("sh")("git push --delete origin defaultBranch")
        1 * getPipelineMock("sh")("git push origin defaultBranch")
        1 * getPipelineMock("sh")(['script':"../gh api -XPATCH 'repos/author/repo' -f default_branch=tempBranch | jq '.default_branch'", 'returnStdout':true]) >> "tempBranch"
        1 * getPipelineMock("sh")(['script':"../gh api -XPATCH 'repos/author/repo' -f default_branch=defaultBranch | jq '.default_branch'", 'returnStdout':true]) >> "defaultBranch"
        1 * getPipelineMock( "echo" )("[INFO] author/repo's default branch has been updated to tempBranch.")
        1 * getPipelineMock( "echo" )("[INFO] author/repo's default branch has been updated to defaultBranch.")

    }
}
