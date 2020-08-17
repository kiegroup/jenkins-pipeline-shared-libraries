import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import groovy.json.JsonSlurper
import hudson.plugins.git.GitSCM

class GithubScmSpec extends JenkinsPipelineSpecification {
    def groovyScript = null
    def pullRequestInfo = null
    def pullRequestInfoEmpty = null
    def forkListInfo = null
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
        explicitlyMockPipelineVariable("GIT_USERNAME")
        explicitlyMockPipelineVariable("GIT_PASSWORD")

        groovyScript.getBinding().setVariable("OAUTHTOKEN", 'oauth_token')
        pullRequestInfo = mockJson('/pull_request_not_empty.json')
        pullRequestInfoEmpty = mockJson('/pull_request_empty.json')
        forkListInfo = mockJson('/forked_projects.json')
        forkListInfoEmpty = mockJson('/forked_projects_empty.json')
    }

    def mockJson(def fileName) {
        def url = getClass().getResource(fileName)
        def data = new File(url.toURI()).text
        getPipelineMock("readJSON")(['text': data]) >> jsonSlurper.parseText(data)
        return data
    }

    def "[githubscm.groovy] resolveRepository"() {
        when:
        groovyScript.resolveRepository('repository', 'author', 'branches', true)
        then:
        1 * getPipelineMock("github.call")(['credentialsId': 'kie-ci', 'repoOwner': 'author', 'repository': 'repository', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock("resolveScm")(['source': 'github', 'ignoreErrors': true, 'targets': ['branches']])
    }

    def "[githubscm.groovy] resolveRepository with different credentials"() {
        when:
        groovyScript.resolveRepository('repository', 'author', 'branches', true, 'ci-usernamePassword')
        then:
        1 * getPipelineMock("github.call")(['credentialsId': 'ci-usernamePassword', 'repoOwner': 'author', 'repository': 'repository', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock("resolveScm")(['source': 'github', 'ignoreErrors': true, 'targets': ['branches']])
    }

    def "[githubscm.groovy] checkoutIfExists without merge"() {
        setup:
        GitSCM gitSCM = new GitSCM('url')
        when:
        groovyScript.checkoutIfExists('repository', 'author', 'branches', 'defaultAuthor', 'master')
        then:
        1 * getPipelineMock("github.call")(['credentialsId': 'kie-ci', 'repoOwner': 'author', 'repository': 'repository', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock("resolveScm")(['source': 'github', 'ignoreErrors': true, 'targets': ['branches']]) >> gitSCM
        1 * getPipelineMock("checkout")(gitSCM)
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/pulls?head=author:branches&state=open'"]) >> pullRequestInfo
    }

    def "[githubscm.groovy] checkoutIfExists first repo does not exist"() {
        setup:
        GitSCM gitSCM = new GitSCM('url')
        when:
        groovyScript.checkoutIfExists('repository', 'author', 'branches', 'defaultAuthor', 'master')
        then:
        1 * getPipelineMock("github.call")(['credentialsId': 'kie-ci', 'repoOwner': 'author', 'repository': 'repository', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock("resolveScm")(['source': 'github', 'ignoreErrors': true, 'targets': ['branches']]) >> null
        0 * getPipelineMock("checkout")(null)

        1 * getPipelineMock("github.call")(['credentialsId': 'kie-ci', 'repoOwner': 'defaultAuthor', 'repository': 'repository', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'defaultGithub'
        1 * getPipelineMock("resolveScm")(['source': 'defaultGithub', 'ignoreErrors': true, 'targets': ['branches']]) >> gitSCM
        1 * getPipelineMock("checkout")(gitSCM)
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/pulls?head=author:branches&state=open'"]) >> pullRequestInfo
    }

    def "[githubscm.groovy] checkoutIfExists with merge true"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        GitSCM repositoryScmInformation = new GitSCM('url1')
        GitSCM repositoryScmInformationMaster = new GitSCM('url2')
        when:
        groovyScript.checkoutIfExists('repository', 'author', 'branches', 'defaultAuthor', 'master', true)
        then:
        1 * getPipelineMock("github.call")(['credentialsId': 'kie-ci', 'repoOwner': 'author', 'repository': 'repository', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock("github.call")(['credentialsId': 'kie-ci', 'repoOwner': 'defaultAuthor', 'repository': 'repository', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock('resolveScm')(['source': 'github', 'ignoreErrors': true, 'targets': ['branches']]) >> repositoryScmInformation
        0 * getPipelineMock('checkout')(repositoryScmInformation)
        1 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'kie-ci', variable: 'kieCiUserPassword']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock('resolveScm')(['source': 'github', 'ignoreErrors': false, 'targets': ['master']]) >> repositoryScmInformationMaster
        1 * getPipelineMock('checkout')(repositoryScmInformationMaster)
        1 * getPipelineMock('sh')('git pull https://user:password@github.com/author/repository branches')
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1']) >> 'git commit information'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/pulls?head=author:branches&state=open'"]) >> pullRequestInfo
    }

    def "[githubscm.groovy] checkoutIfExists with merge true and different forked project name"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        GitSCM repositoryScmInformation = new GitSCM('url1')
        GitSCM repositoryScmInformationMaster = new GitSCM('url2')
        when:
        groovyScript.checkoutIfExists('repository', 'irtyamine', 'branches', 'defaultAuthor', 'master', true)
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/forks'"]) >> forkListInfo
        1 * getPipelineMock("github.call")(['credentialsId': 'kie-ci', 'repoOwner': 'irtyamine', 'repository': 'github-action-build-chain', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock("github.call")(['credentialsId': 'kie-ci', 'repoOwner': 'defaultAuthor', 'repository': 'repository', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock('resolveScm')(['source': 'github', 'ignoreErrors': true, 'targets': ['branches']]) >> repositoryScmInformation
        0 * getPipelineMock('checkout')(repositoryScmInformation)
        1 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'kie-ci', variable: 'kieCiUserPassword']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock('resolveScm')(['source': 'github', 'ignoreErrors': false, 'targets': ['master']]) >> repositoryScmInformationMaster
        1 * getPipelineMock('checkout')(repositoryScmInformationMaster)
        1 * getPipelineMock('sh')('git pull https://user:password@github.com/irtyamine/github-action-build-chain branches')
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1']) >> 'git commit information'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/pulls?head=irtyamine:branches&state=open'"]) >> pullRequestInfo
    }

    def "[githubscm.groovy] checkoutIfExists with merge true and different credentials"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        GitSCM repositoryScmInformation = new GitSCM('url1')
        GitSCM repositoryScmInformationMaster = new GitSCM('url2')
        when:
        groovyScript.checkoutIfExists('repository', 'author', 'branches', 'defaultAuthor', 'master', true, ['token': 'ci-token', 'usernamePassword': 'ci-usernamePassword'])
        then:
        1 * getPipelineMock("github.call")(['credentialsId': 'ci-usernamePassword', 'repoOwner': 'author', 'repository': 'repository', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock("github.call")(['credentialsId': 'ci-usernamePassword', 'repoOwner': 'defaultAuthor', 'repository': 'repository', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock('resolveScm')(['source': 'github', 'ignoreErrors': true, 'targets': ['branches']]) >> repositoryScmInformation
        0 * getPipelineMock('checkout')(repositoryScmInformation)
        1 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'ci-usernamePassword', variable: 'kieCiUserPassword']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock('resolveScm')(['source': 'github', 'ignoreErrors': false, 'targets': ['master']]) >> repositoryScmInformationMaster
        1 * getPipelineMock('checkout')(repositoryScmInformationMaster)
        1 * getPipelineMock('sh')('git pull https://user:password@github.com/author/repository branches')
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1']) >> 'git commit information'
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/pulls?head=author:branches&state=open'"]) >> pullRequestInfo
        2 * getPipelineMock("string.call")(['credentialsId': 'ci-token', 'variable': 'OAUTHTOKEN'])
    }

    def "[githubscm.groovy] checkoutIfExists has not PR"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        GitSCM repositoryScmInformation = new GitSCM('url1')
        GitSCM repositoryScmInformationMaster = new GitSCM('url2')
        when:
        groovyScript.checkoutIfExists('repository', 'author', 'branches', 'defaultAuthor', 'master', true)
        then:
        1 * getPipelineMock("github.call")(['credentialsId': 'kie-ci', 'repoOwner': 'author', 'repository': 'repository', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock("github.call")(['credentialsId': 'kie-ci', 'repoOwner': 'defaultAuthor', 'repository': 'repository', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'github'

        1 * getPipelineMock('resolveScm')(['source': 'github', 'ignoreErrors': true, 'targets': ['branches']]) >> repositoryScmInformation
        0 * getPipelineMock('checkout')(repositoryScmInformation)
        0 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'kie-ci', variable: 'kieCiUserPassword']) >> 'userNamePassword'

        1 * getPipelineMock('resolveScm')(['source': 'github', 'ignoreErrors': false, 'targets': ['master']]) >> repositoryScmInformationMaster
        1 * getPipelineMock('checkout')(repositoryScmInformationMaster)
        0 * getPipelineMock('sh')('git pull https://user:password@github.com/author/repository branches')

        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/pulls?head=author:branches&state=open'"]) >> pullRequestInfoEmpty
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/defaultAuthor/repository/pulls?head=defaultAuthor:branches&state=open'"]) >> pullRequestInfoEmpty
    }

    def "[githubscm.groovy] checkoutIfExists Multibranch pipeline job"() {
        setup:
        GitSCM gitSCM = new GitSCM('url')
        when:
        groovyScript.checkoutIfExists('repository', 'kiegroup', 'master', 'kiegroup', 'master')
        then:
        2 * getPipelineMock("github.call")(['credentialsId': 'kie-ci', 'repoOwner': 'kiegroup', 'repository': 'repository', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock("resolveScm")(['source': 'github', 'ignoreErrors': true, 'targets': ['master']]) >> gitSCM
        1 * getPipelineMock("resolveScm")(['source': 'github', 'ignoreErrors': false, 'targets': ['master']]) >> gitSCM
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/kiegroup/repository/pulls?head=kiegroup:master&state=open'"]) >> pullRequestInfoEmpty
        1 * getPipelineMock("checkout")(gitSCM)
        0 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1'])
    }

    def "[githubscm.groovy] getRepositoryScm"() {
        setup:
        GitSCM repositoryScmInformation = new GitSCM('url')
        when:
        def result = groovyScript.getRepositoryScm('repository', 'author', 'branches')
        then:
        1 * getPipelineMock("github.call")(['credentialsId': 'kie-ci', 'repoOwner': 'author', 'repository': 'repository', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock('resolveScm')(['source': 'github', 'ignoreErrors': true, 'targets': ['branches']]) >> repositoryScmInformation
        result == repositoryScmInformation
    }

    def "[githubscm.groovy] getRepositoryScm with different credentials"() {
        setup:
        GitSCM repositoryScmInformation = new GitSCM('url')
        when:
        def result = groovyScript.getRepositoryScm('repository', 'author', 'branches', 'ci-usernamePassword')
        then:
        1 * getPipelineMock("github.call")(['credentialsId': 'ci-usernamePassword', 'repoOwner': 'author', 'repository': 'repository', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock('resolveScm')(['source': 'github', 'ignoreErrors': true, 'targets': ['branches']]) >> repositoryScmInformation
        result == repositoryScmInformation
    }

    def "[githubscm.groovy] getRepositoryScm exception"() {
        when:
        def result = groovyScript.getRepositoryScm('repository', 'author', 'branches')
        then:
        1 * getPipelineMock("github.call")(['credentialsId': 'kie-ci', 'repoOwner': 'author', 'repository': 'repository', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock('resolveScm')(['source': 'github', 'ignoreErrors': true, 'targets': ['branches']]) >> { throw new Exception('exception') }
        result == null
    }

    def "[githubscm.groovy] mergeSourceIntoTarget"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        GitSCM repositoryScmInformation = new GitSCM('url')
        when:
        groovyScript.mergeSourceIntoTarget('sourceRepository', 'sourceAuthor', 'sourceBranches', 'targetRepository', 'targetAuthor', 'targetBranches')
        then:
        1 * getPipelineMock("github.call")(['credentialsId': 'kie-ci', 'repoOwner': 'targetAuthor', 'repository': 'targetRepository', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock('resolveScm')(['source': 'github', 'ignoreErrors': false, 'targets': ['targetBranches']]) >> repositoryScmInformation
        1 * getPipelineMock('checkout')(repositoryScmInformation)
        1 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'kie-ci', variable: 'kieCiUserPassword']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock('sh')('git pull https://user:password@github.com/sourceAuthor/sourceRepository sourceBranches')
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1']) >> 'git commit information'
    }

    def "[githubscm.groovy] mergeSourceIntoTarget with different credentialsID"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        GitSCM repositoryScmInformation = new GitSCM('url')
        when:
        groovyScript.mergeSourceIntoTarget('sourceRepository', 'sourceAuthor', 'sourceBranches', 'targetRepository', 'targetAuthor', 'targetBranches', 'ci-usernamePassword')
        then:
        1 * getPipelineMock("github.call")(['credentialsId': 'ci-usernamePassword', 'repoOwner': 'targetAuthor', 'repository': 'targetRepository', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock('resolveScm')(['source': 'github', 'ignoreErrors': false, 'targets': ['targetBranches']]) >> repositoryScmInformation
        1 * getPipelineMock('checkout')(repositoryScmInformation)
        1 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'ci-usernamePassword', variable: 'kieCiUserPassword']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock('sh')('git pull https://user:password@github.com/sourceAuthor/sourceRepository sourceBranches')
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1']) >> 'git commit information'
    }

    def "[githubscm.groovy] mergeSourceIntoTarget throw exception"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        GitSCM repositoryScmInformation = new GitSCM('url')
        when:
        groovyScript.mergeSourceIntoTarget('sourceRepository', 'sourceAuthor', 'sourceBranches', 'targetRepository', 'targetAuthor', 'targetBranches')
        then:
        1 * getPipelineMock("github.call")(['credentialsId': 'kie-ci', 'repoOwner': 'targetAuthor', 'repository': 'targetRepository', 'traits': [['$class': 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId': 3], ['$class': 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId': 1], ['$class': 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId': 1, 'trust': ['$class': 'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock('resolveScm')(['source': 'github', 'ignoreErrors': false, 'targets': ['targetBranches']]) >> repositoryScmInformation
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

    def "[githubscm.groovy] forkRepo without credentials"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_PASSWORD", 'password')
        when:
        groovyScript.forkRepo()
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'kie-ci', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASSWORD']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)

        1 * getPipelineMock("sh")('git config --global hub.protocol https')
        1 * getPipelineMock("sh")('hub fork --remote-name=origin')
        1 * getPipelineMock("sh")('git remote -v')
    }

    def "[githubscm.groovy] forkRepo with credentials"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_PASSWORD", 'password')
        when:
        groovyScript.forkRepo('credentialsId')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'credentialsId', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASSWORD']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)

        1 * getPipelineMock("sh")('git config --global hub.protocol https')
        1 * getPipelineMock("sh")('hub fork --remote-name=origin')
        1 * getPipelineMock("sh")('git remote -v')
    }

    def "[githubscm.groovy] createPR without body, Credentials and target branch"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_PASSWORD", 'password')
        when:
        def result = groovyScript.createPR('PR Title')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'kie-ci', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASSWORD']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hub pull-request -m 'PR Title' -m '' -b 'master'"]) >> 'shResult'
    }

    def "[githubscm.groovy] createPR without Credentials and target branch"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_PASSWORD", 'password')
        when:
        def result = groovyScript.createPR('PR Title', 'PR body.')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'kie-ci', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASSWORD']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hub pull-request -m 'PR Title' -m 'PR body.' -b 'master'"]) >> 'shResult'
    }

    def "[githubscm.groovy] createPR without Credentials and target branch throwing exception"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_PASSWORD", 'password')
        when:
        def result = groovyScript.createPR('PR Title')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'kie-ci', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASSWORD']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hub pull-request -m 'PR Title' -m '' -b 'master'"]) >> { throw new Exception('error') }
        thrown(Exception)
    }

    def "[githubscm.groovy] createPR with body, Credentials and target branch"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_PASSWORD", 'password')
        when:
        def result = groovyScript.createPR('PR Title', 'PR body.', 'targetBranch', 'credentialsId')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'credentialsId', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASSWORD']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hub pull-request -m 'PR Title' -m 'PR body.' -b 'targetBranch'"]) >> 'shResult'
    }

    def "[githubscm.groovy] mergePR without Credentials"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_PASSWORD", 'password')
        when:
        groovyScript.mergePR('pullRequestLink')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'kie-ci', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASSWORD']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('hub merge pullRequestLink')
    }

    def "[githubscm.groovy] mergePR without Credentials throwing exception"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_PASSWORD", 'password')
        when:
        groovyScript.mergePR('pullRequestLink')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'kie-ci', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASSWORD']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")('hub merge pullRequestLink') >> { throw new Exception('hub error') }
        thrown(Exception)
    }

    def "[githubscm.groovy] mergePR with Credentials"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_PASSWORD", 'password')
        when:
        groovyScript.mergePR('pullRequestLink', 'credentialsId')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'credentialsId', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASSWORD']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
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
        when:
        groovyScript.pushObject('remote', 'object')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'kie-ci', 'usernameVariable': 'GIT_USERNAME', 'passwordVariable': 'GIT_PASSWORD'])
        1 * getPipelineMock("withCredentials")(_)
        1 * getPipelineMock("sh")("git config --local credential.helper \"!f() { echo username=\\$GIT_USERNAME; echo password=\\$GIT_PASSWORD; }; f\"")
        1 * getPipelineMock("sh")("git push remote object")
    }

    def "[githubscm.groovy] pushObject with credentialsId"() {
        when:
        groovyScript.pushObject('remote', 'object', 'credsId')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'credsId', 'usernameVariable': 'GIT_USERNAME', 'passwordVariable': 'GIT_PASSWORD'])
        1 * getPipelineMock("withCredentials")(_)
        1 * getPipelineMock("sh")("git config --local credential.helper \"!f() { echo username=\\$GIT_USERNAME; echo password=\\$GIT_PASSWORD; }; f\"")
        1 * getPipelineMock("sh")("git push remote object")
    }

    def "[githubscm.groovy] pushObject exception"() {
        setup:
        getPipelineMock("sh")("git push remote object") >> {
            throw new Exception("error when pushing")
        }
        when:
        groovyScript.pushObject('remote', 'object')
        then:
        1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'kie-ci', 'usernameVariable': 'GIT_USERNAME', 'passwordVariable': 'GIT_PASSWORD'])
        1 * getPipelineMock("withCredentials")(_)
        1 * getPipelineMock("sh")("git config --local credential.helper \"!f() { echo username=\\$GIT_USERNAME; echo password=\\$GIT_PASSWORD; }; f\"")
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
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks'"]) >> forkListInfo
        'github-action-build-chain' == result
    }

    def "[githubscm.groovy] getForkedProject no existing"() {
        when:
        def result = groovyScript.getForkedProjectName('groupx', 'repox', 'unknownuser')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks'"]) >> forkListInfo
        null == result
    }

    def "[githubscm.groovy] getForkedProject empty"() {
        when:
        def result = groovyScript.getForkedProjectName('groupx', 'repox', 'irtyamine')
        then:
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "curl -H \"Authorization: token oauth_token\" 'https://api.github.com/repos/groupx/repox/forks'"]) >> forkListInfoEmpty
        null == result
    }
}
