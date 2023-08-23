import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import org.jenkinsci.plugins.workflow.steps.Step
import org.jenkinsci.plugins.workflow.steps.StepContext
import org.jenkinsci.plugins.workflow.steps.StepExecution
import org.kie.jenkins.shell.SSHShell
import org.kie.jenkins.shell.installation.Installation

class SSHShellSpec extends JenkinsPipelineSpecification {

    def steps
    def env = [:]

    def setup() {
        steps = new Step() {

            @Override
            StepExecution start(StepContext stepContext) throws Exception {
                return null
            }

        }
    }

    def "[SSHShell.groovy] getFullCommand default"() {
        setup:
        1 * getPipelineMock('sh')([returnStdout: true, script: 'mktemp -d']) >> 'TMP_FOLDER'
        def shell = new SSHShell(steps, 'SERVER')
        when:
        def result = shell.getFullCommand('whatever')
        then:
        result == "ssh  SERVER \"whatever\""
    }

    def "[SSHShell.groovy] getFullCommand with options, installations and environment variables"() {
        setup:
        def install1 = Mock(Installation)
        def install2 = Mock(Installation)
        1 * getPipelineMock('sh')([returnStdout: true, script: 'mktemp -d']) >> 'TMP_FOLDER'
        def shell = new SSHShell(steps, 'SERVER', 'SSH_OPTIONS')
        shell.install(install1)
        shell.install(install2)
        shell.addEnvironmentVariable('KEY1', 'VALUE1')
        shell.addEnvironmentVariable('key2', 'value2')
        1 * install1.getBinaryPaths() >> ['PATH1', 'PATH2']
        1 * install2.getBinaryPaths() >> ['PATH3']
        1 * install1.getExtraEnvVars() >> [:]
        1 * install2.getExtraEnvVars() >> [ install2key : 'install2value' ]
        when:
        def result = shell.getFullCommand('whatever')
        then:
        result == """ssh SSH_OPTIONS SERVER \"export PATH=\${PATH}:PATH1:PATH2:PATH3
export install2key=install2value
export KEY1=VALUE1
export key2=value2
whatever\""""
    }

}
