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

    def "[SSHShell.groovy] getFullCommand ssh options"() {
        setup:
        1 * getPipelineMock('sh')([returnStdout: true, script: 'mktemp -d']) >> 'TMP_FOLDER'
        def shell = new SSHShell(steps, 'SERVER', 'SSH_OPTIONS')
        when:
        def result = shell.getFullCommand('whatever')
        then:
        result == "ssh SSH_OPTIONS SERVER \"whatever\""
    }

    def "[LocalShell.groovy] getFullCommand with installations"() {
        setup:
        def install1 = Mock(Installation)
        def install2 = Mock(Installation)
        1 * getPipelineMock('sh')([returnStdout: true, script: 'mktemp -d']) >> 'TMP_FOLDER'
        def shell = new SSHShell(steps, 'SERVER')
        shell.install(install1)
        shell.install(install2)
        1 * install1.getBinaryPaths() >> ['PATH1', 'PATH2']
        1 * install2.getBinaryPaths() >> ['PATH3']
        1 * install1.getExtraEnvVars() >> [:]
        1 * install2.getExtraEnvVars() >> [ install2key : 'install2value' ]
        when:
        def result = shell.getFullCommand('whatever')
        then:
        result == """ssh  SERVER \"export PATH=\${PATH}:PATH1:PATH2:PATH3
export install2key=install2value
whatever\""""
    }

    def "[LocalShell.groovy] getFullCommand with environment variables"() {
        setup:
        1 * getPipelineMock('sh')([returnStdout: true, script: 'mktemp -d']) >> 'TMP_FOLDER'
        def shell = new SSHShell(steps, 'SERVER')
        shell.addEnvironmentVariable('KEY1', 'VALUE1')
        shell.addEnvironmentVariable('key2', 'value2')
        when:
        def result = shell.getFullCommand('whatever')
        then:
        result == """ssh  SERVER \"export KEY1=VALUE1
export key2=value2
whatever\""""
    }

    def "[LocalShell.groovy] getFullCommand with directory"() {
        setup:
        1 * getPipelineMock('sh')([returnStdout: true, script: 'mktemp -d']) >> 'TMP_FOLDER'
        def shell = new SSHShell(steps, 'SERVER')
        when:
        def result = shell.getFullCommand('whatever', 'DIR')
        then:
        result == """ssh  SERVER \"mkdir -p DIR && cd DIR
whatever\""""
    }

    def "[SSHShell.groovy] getFullCommand with all"() {
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
        def result = shell.getFullCommand('whatever', 'DIR')
        then:
        result == """ssh SSH_OPTIONS SERVER \"mkdir -p DIR && cd DIR
export PATH=\${PATH}:PATH1:PATH2:PATH3
export install2key=install2value
export KEY1=VALUE1
export key2=value2
whatever\""""
    }

    def "[SSHShell.groovy] copyFilesFromRemote"() {
        setup:
        def shell = new SSHShell(steps, 'SERVER', 'SSH_OPTIONS')
        when:
        shell.copyFilesFromRemote('REMOTE', 'LOCAL')
        then:
        1 * getPipelineMock('sh')("scp SSH_OPTIONS SERVER:REMOTE LOCAL")
    }

    def "[SSHShell.groovy] copyFilesToRemote"() {
        setup:
        def shell = new SSHShell(steps, 'SERVER', 'SSH_OPTIONS')
        when:
        shell.copyFilesToRemote('LOCAL', 'REMOTE')
        then:
        1 * getPipelineMock('sh')("scp SSH_OPTIONS LOCAL SERVER:REMOTE")
    }

}
