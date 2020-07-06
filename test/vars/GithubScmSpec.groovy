import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import hudson.plugins.git.GitSCM

class GithubScmSpec extends JenkinsPipelineSpecification {
    def groovyScript = null

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
    }

    def "[githubscm.groovy] resolveRepository"() {
        when:
        groovyScript.resolveRepository('repository', 'author', 'branches', true)
        then:
        1 * getPipelineMock("github.call")(['credentialsId':'kie-ci', 'repoOwner':'author', 'repository':'repository', 'traits':[['$class':'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId':3], ['$class':'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId':1], ['$class':'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId':1, 'trust':['$class':'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock("resolveScm")(['source':'github', 'ignoreErrors':true, 'targets':['branches']])
    }

    def "[githubscm.groovy] checkoutIfExists without merge"() {
        when:
        groovyScript.checkoutIfExists('repository', 'author', 'branches', 'author', 'master')
        then:

        1 * getPipelineMock("github.call")(['credentialsId':'kie-ci', 'repoOwner':'author', 'repository':'repository', 'traits':[['$class':'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId':3], ['$class':'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId':1], ['$class':'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId':1, 'trust':['$class':'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock("resolveScm")(['source':'github', 'ignoreErrors':true, 'targets':['branches']]) >> 'repositoryScmInformation'
        1 * getPipelineMock("checkout")('repositoryScmInformation')
    }

    def "[githubscm.groovy] checkoutIfExists first repo does not exist"() {
        when:
        groovyScript.checkoutIfExists('repository', 'author', 'branches', 'defaultAuthor', 'master')
        then:

        1 * getPipelineMock("github.call")(['credentialsId':'kie-ci', 'repoOwner':'author', 'repository':'repository', 'traits':[['$class':'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId':3], ['$class':'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId':1], ['$class':'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId':1, 'trust':['$class':'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock("resolveScm")(['source':'github', 'ignoreErrors':true, 'targets':['branches']]) >> null
        0 * getPipelineMock("checkout")(null)

        1 * getPipelineMock("github.call")(['credentialsId':'kie-ci', 'repoOwner':'defaultAuthor', 'repository':'repository', 'traits':[['$class':'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId':3], ['$class':'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId':1], ['$class':'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId':1, 'trust':['$class':'TrustPermission']]]]) >> 'defaultGithub'
        1 * getPipelineMock("resolveScm")(['source':'defaultGithub', 'ignoreErrors':true, 'targets':['branches']]) >> 'defaultScm'
        1 * getPipelineMock("checkout")('defaultScm')
    }

    def "[githubscm.groovy] checkoutIfExists with merge true"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        when:
        groovyScript.checkoutIfExists('repository', 'author', 'branches', 'author', 'master', true)
        then:
        2 * getPipelineMock("github.call")(['credentialsId':'kie-ci', 'repoOwner':'author', 'repository':'repository', 'traits':[['$class':'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId':3], ['$class':'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId':1], ['$class':'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId':1, 'trust':['$class':'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock('resolveScm')(['source':'github', 'ignoreErrors':true, 'targets':['branches']]) >> 'repositoryScmInformation'
        0 * getPipelineMock('checkout')('repositoryScmInformation')
        1 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'kie-ci', variable: 'kieCiUserPassword']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock('resolveScm')(['source':'github', 'ignoreErrors':false, 'targets':['master']]) >> 'repositoryScmInformationMaster'
        1 * getPipelineMock('checkout')('repositoryScmInformationMaster')
        1 * getPipelineMock('sh')('git pull https://user:password@github.com/author/repository branches')
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1']) >> 'git commit information'
    }

    def "[githubscm.groovy] getRepositoryScm"() {
        when:
        def result = groovyScript.getRepositoryScm('repository', 'author', 'branches')
        then:
        1 * getPipelineMock("github.call")(['credentialsId':'kie-ci', 'repoOwner':'author', 'repository':'repository', 'traits':[['$class':'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId':3], ['$class':'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId':1], ['$class':'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId':1, 'trust':['$class':'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock('resolveScm')(['source':'github', 'ignoreErrors':true, 'targets':['branches']]) >> 'repositoryScmInformation'
        result == 'repositoryScmInformation'
    }

    def "[githubscm.groovy] getRepositoryScm exception"() {
        when:
        def result = groovyScript.getRepositoryScm('repository', 'author', 'branches')
        then:
        1 * getPipelineMock("github.call")(['credentialsId':'kie-ci', 'repoOwner':'author', 'repository':'repository', 'traits':[['$class':'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId':3], ['$class':'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId':1], ['$class':'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId':1, 'trust':['$class':'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock('resolveScm')(['source':'github', 'ignoreErrors':true, 'targets':['branches']]) >> { throw new Exception('exception') }
        result == null
    }

    def "[githubscm.groovy] mergeSourceIntoTarget"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        when:
        groovyScript.mergeSourceIntoTarget('repository', 'sourceAuthor', 'sourceBranches', 'targetAuthor', 'targetBranches')
        then:
        1 * getPipelineMock("github.call")(['credentialsId':'kie-ci', 'repoOwner':'targetAuthor', 'repository':'repository', 'traits':[['$class':'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId':3], ['$class':'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId':1], ['$class':'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId':1, 'trust':['$class':'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock('resolveScm')(['source':'github', 'ignoreErrors':false, 'targets':['targetBranches']]) >> 'repositoryScmInformation'
        1 * getPipelineMock('checkout')('repositoryScmInformation')
        1 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'kie-ci', variable: 'kieCiUserPassword']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock('sh')('git pull https://user:password@github.com/sourceAuthor/repository sourceBranches')
        2 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1']) >> 'git commit information'
    }

    def "[githubscm.groovy] mergeSourceIntoTarget throw exception"() {
        setup:
        groovyScript.getBinding().setVariable("kieCiUserPassword", 'user:password')
        when:
        groovyScript.mergeSourceIntoTarget('repository', 'sourceAuthor', 'sourceBranches', 'targetAuthor', 'targetBranches')
        then:
        1 * getPipelineMock("github.call")(['credentialsId':'kie-ci', 'repoOwner':'targetAuthor', 'repository':'repository', 'traits':[['$class':'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', 'strategyId':3], ['$class':'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', 'strategyId':1], ['$class':'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', 'strategyId':1, 'trust':['$class':'TrustPermission']]]]) >> 'github'
        1 * getPipelineMock('resolveScm')(['source':'github', 'ignoreErrors':false, 'targets':['targetBranches']]) >> 'repositoryScmInformation'
        1 * getPipelineMock('checkout')('repositoryScmInformation')
        1 * getPipelineMock('usernameColonPassword.call')([credentialsId: 'kie-ci', variable: 'kieCiUserPassword']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock('sh')('git pull https://user:password@github.com/sourceAuthor/repository sourceBranches') >> { throw new Exception('git error')}
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
        1 * getPipelineMock("sh")('git checkout -b branchName') >> { throw new Exception('git error')}
        thrown(Exception)
    }

    def "[githubscm.groovy] commitChanges without files to add"() {
        when:
        groovyScript.commitChanges('userName', 'user@email.com', 'commit message')
        then:
        1 * getPipelineMock("sh")("git config user.name 'userName'")
        1 * getPipelineMock("sh")("git config user.email 'user@email.com'")
        1 * getPipelineMock("sh")('git add --all')
        1 * getPipelineMock("sh")("git commit -m 'commit message'")
    }

    def "[githubscm.groovy] commitChanges with files to add"() {
        when:
        groovyScript.commitChanges('userName', 'user@email.com', 'commit message', 'src/*')
        then:
        1 * getPipelineMock("sh")("git config user.name 'userName'")
        1 * getPipelineMock("sh")("git config user.email 'user@email.com'")
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

    def "[githubscm.groovy] createPR without Credentials and target branch"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_PASSWORD", 'password')
        when:
        def result = groovyScript.createPR('pullRequestMessage')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'kie-ci', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASSWORD']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hub pull-request -m 'pullRequestMessage' -b 'master'"]) >> 'shResult'
    }

    def "[githubscm.groovy] createPR without Credentials and target branch throwing exception"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_PASSWORD", 'password')
        when:
        def result = groovyScript.createPR('pullRequestMessage')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'kie-ci', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASSWORD']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hub pull-request -m 'pullRequestMessage' -b 'master'"]) >> { throw new Exception('error') }
        thrown(Exception)
    }

    def "[githubscm.groovy] createPR with Credentials and target branch"() {
        setup:
        groovyScript.getBinding().setVariable("GITHUB_USER", 'user')
        groovyScript.getBinding().setVariable("GITHUB_PASSWORD", 'password')
        when:
        def result = groovyScript.createPR('pullRequestMessage', 'targetBranch', 'credentialsId')
        then:
        1 * getPipelineMock("sh")("rm -rf ~/.config/hub")
        1 * getPipelineMock('usernamePassword.call')([credentialsId: 'credentialsId', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASSWORD']) >> 'userNamePassword'
        1 * getPipelineMock("withCredentials")(['userNamePassword'], _ as Closure)
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': "hub pull-request -m 'pullRequestMessage' -b 'targetBranch'"]) >> 'shResult'
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
        1 * getPipelineMock("sh")('hub merge pullRequestLink') >> { throw new Exception('hub error')}
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
        groovyScript.tagRepository('userName', 'user@email.com', 'tagName', 'buildTag')
        then:
        1 * getPipelineMock("sh")("git config user.name 'userName'")
        1 * getPipelineMock("sh")("git config user.email 'user@email.com'")
        1 * getPipelineMock("sh")("git tag -a 'tagName' -m 'Tagged by Jenkins in build \"buildTag\".'")
    }

    def "[githubscm.groovy] tagRepository without buildTag"() {
        when:
        groovyScript.tagRepository('userName', 'user@email.com', 'tagName')
        then:
        1 * getPipelineMock("sh")("git config user.name 'userName'")
        1 * getPipelineMock("sh")("git config user.email 'user@email.com'")
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
        1 * getPipelineMock("sh")(['returnStdout': true, 'script': 'git log --oneline -1']) >> { return 'cd13a88 (HEAD -> BXMSPROD-819, upstream/master, upstream/HEAD, master) [KOGITO-2285] Shared libaries: Git tagging (#41)' }
        result == 'cd13a88 (HEAD -> BXMSPROD-819, upstream/master, upstream/HEAD, master) [KOGITO-2285] Shared libaries: Git tagging (#41)'
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
}
