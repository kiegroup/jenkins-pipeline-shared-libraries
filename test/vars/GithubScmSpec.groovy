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
            1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'kie-ci', 'usernameVariable':'GIT_USERNAME', 'passwordVariable':'GIT_PASSWORD'])
            1 * getPipelineMock("withCredentials")(_)
            1 * getPipelineMock("sh")("git config --local credential.helper \"!f() { echo username=\\$GIT_USERNAME; echo password=\\$GIT_PASSWORD; }; f\"")
            1 * getPipelineMock("sh")("git push remote object")
    }

    def "[githubscm.groovy] pushObject with credentialsId"() {
        when:
            groovyScript.pushObject('remote', 'object', 'credsId')
        then:
            1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'credsId', 'usernameVariable':'GIT_USERNAME', 'passwordVariable':'GIT_PASSWORD'])
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
            1 * getPipelineMock("usernamePassword.call").call(['credentialsId': 'kie-ci', 'usernameVariable':'GIT_USERNAME', 'passwordVariable':'GIT_PASSWORD'])
            1 * getPipelineMock("withCredentials")(_)
            1 * getPipelineMock("sh")("git config --local credential.helper \"!f() { echo username=\\$GIT_USERNAME; echo password=\\$GIT_PASSWORD; }; f\"")
            thrown(Exception)
    }
}
