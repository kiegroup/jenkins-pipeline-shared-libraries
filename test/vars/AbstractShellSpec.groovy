import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import org.jenkinsci.plugins.workflow.steps.Step
import org.jenkinsci.plugins.workflow.steps.StepContext
import org.jenkinsci.plugins.workflow.steps.StepExecution
import org.kie.jenkins.shell.AbstractShell
import org.kie.jenkins.shell.installation.Installation

class AbstractShellSpec extends JenkinsPipelineSpecification {

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

    class DummyShell extends AbstractShell {
        DummyShell(def script, String installationDir = '', String cpuArchitecture = '') {
            super(script, installationDir, cpuArchitecture)
        }

        @Override
        String getFullCommand(String command, String directory) {
            return "${directory}${command}"
        }
    }

    def "[AbstractShell.groovy] execute"() {
        setup:
        1 * getPipelineMock('sh')([returnStdout: true, script: 'mktemp -d']) >> 'TMP_FOLDER'
        def shell = new DummyShell(steps)
        when:
        shell.execute('whatever')
        then:
        1 * getPipelineMock('sh')("whatever")
    }

    def "[AbstractShell.groovy] execute with directory"() {
        setup:
        1 * getPipelineMock('sh')([returnStdout: true, script: 'mktemp -d']) >> 'TMP_FOLDER'
        def shell = new DummyShell(steps)
        when:
        shell.execute('whatever', 'DIR')
        then:
        1 * getPipelineMock('sh')("DIRwhatever")
    }

    def "[AbstractShell.groovy] executeWithOutput"() {
        setup:
        1 * getPipelineMock('sh')([returnStdout: true, script: 'mktemp -d']) >> 'TMP_FOLDER'
        def shell = new DummyShell(steps)
        when:
        def result = shell.executeWithOutput('whatever')
        then:
        1 * getPipelineMock('sh')([returnStdout: true, script: "whatever"]) >> 'output '
        result == 'output'
    }

    def "[AbstractShell.groovy] executeWithOutput with directory"() {
        setup:
        1 * getPipelineMock('sh')([returnStdout: true, script: 'mktemp -d']) >> 'TMP_FOLDER'
        def shell = new DummyShell(steps)
        when:
        def result = shell.executeWithOutput('whatever', 'DIR')
        then:
        1 * getPipelineMock('sh')([returnStdout: true, script: "DIRwhatever"]) >> 'output '
        result == 'output'
    }

    def "[AbstractShell.groovy] executeWithStatus"() {
        setup:
        1 * getPipelineMock('sh')([returnStdout: true, script: 'mktemp -d']) >> 'TMP_FOLDER'
        def shell = new DummyShell(steps)
        when:
        def result = shell.executeWithStatus('whatever')
        then:
        1 * getPipelineMock('sh')([returnStatus: true, script: "whatever"]) >> 0
        result == 0
    }

    def "[AbstractShell.groovy] executeWithStatus with directory"() {
        setup:
        1 * getPipelineMock('sh')([returnStdout: true, script: 'mktemp -d']) >> 'TMP_FOLDER'
        def shell = new DummyShell(steps)
        when:
        def result = shell.executeWithStatus('whatever', 'DIR')
        then:
        1 * getPipelineMock('sh')([returnStatus: true, script: "DIRwhatever"]) >> 0
        result == 0
    }

    def "[AbstractShell.groovy] environment variables handling"() {
        setup:
        def install = Mock(Installation)
        1 * getPipelineMock('sh')([returnStdout: true, script: 'mktemp -d']) >> 'TMP_FOLDER'
        def shell = new DummyShell(steps)
        shell.install(install)
        1 * install.getExtraEnvVars() >> [ installkey: 'installvalue' ]
        when:
        shell.addEnvironmentVariable('KEY1', 'VALUE1')
        shell.addEnvironmentVariable('key2', 'value2')
        then:
        shell.getEnvironmentVariables() == [ installkey: 'installvalue', KEY1: 'VALUE1', key2 : 'value2']
    }

    def "[AbstractShell.groovy] install"() {
        setup:
        def install = Mock(Installation)
        1 * getPipelineMock('sh')([returnStdout: true, script: 'mktemp -d']) >> 'TMP_FOLDER'
        def shell = new DummyShell(steps)
        when:
        shell.install(install)
        then:
        shell.installationDir == 'TMP_FOLDER'
        shell.cpuArchitecture == 'amd64'
        1 * install.setCpuArchitecture('amd64')
        1 * install.install('TMP_FOLDER')
        shell.installations == [install]
    }

    def "[AbstractShell.groovy] install with installationDir and cpuArchitecture"() {
        setup:
        def install = Mock(Installation)
        0 * getPipelineMock('sh')([returnStdout: true, script: 'mktemp -d']) >> 'TMP_FOLDER'
        when:
        def shell = new DummyShell(steps, 'DUMMY_FOLDER', 'CPUARCH')
        shell.install(install)
        then:
        shell.installationDir == 'DUMMY_FOLDER'
        shell.cpuArchitecture == 'CPUARCH'
        1 * install.setCpuArchitecture('CPUARCH')
        1 * install.install('DUMMY_FOLDER')
        shell.installations == [install]
    }

    def "[AbstractShell.groovy] install with debug before"() {
        setup:
        def install = Mock(Installation)
        1 * getPipelineMock('sh')([returnStdout: true, script: 'mktemp -d']) >> 'TMP_FOLDER'
        when:
        def shell = new DummyShell(steps)
        shell.enableDebug()
        shell.install(install)
        then:
        shell.installationDir == 'TMP_FOLDER'
        shell.cpuArchitecture == 'amd64'
        shell.debug == true
        1 * install.setCpuArchitecture('amd64')
        1 * install.install('TMP_FOLDER')
        1 * install.enableDebug()
        shell.installations == [install]
    }

    def "[AbstractShell.groovy] install with debug after"() {
        setup:
        def install = Mock(Installation)
        1 * getPipelineMock('sh')([returnStdout: true, script: 'mktemp -d']) >> 'TMP_FOLDER'
        when:
        def shell = new DummyShell(steps)
        shell.install(install)
        shell.enableDebug()
        then:
        shell.installationDir == 'TMP_FOLDER'
        shell.cpuArchitecture == 'amd64'
        shell.debug == true
        1 * install.setCpuArchitecture('amd64')
        1 * install.install('TMP_FOLDER')
        1 * install.enableDebug()
        shell.installations == [install]
    }
}
