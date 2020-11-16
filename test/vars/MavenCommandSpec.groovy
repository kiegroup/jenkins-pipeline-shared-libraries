import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import org.jenkinsci.plugins.workflow.steps.Step
import org.jenkinsci.plugins.workflow.steps.StepContext
import org.jenkinsci.plugins.workflow.steps.StepExecution

class MavenCommandSpec extends JenkinsPipelineSpecification {
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

    def "[MavenCommand.groovy] run simple"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.run('whatever')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B whatever', returnStdout: false])
        0 * getPipelineMock("error")(_)
    }

    def "[MavenCommand.groovy] run with all set"() {
        setup:
        steps.env = ['MAVEN_SETTINGS_XML':'settingsFileId']
        def props = new Properties()
        props.put('key1', 'value1')
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand
            .withProperties(props)
            .withProperty('key2')
            .withSettingsXmlId('anyId')
            .withOptions(['hello', 'bonjour'])
            .withProfiles(['p1'])
            .skipTests(true)
            .withLogFileName('LOG_FILE')
            .withDeployRepository('REPOSITORY')
            .run('whatever')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -s settingsFileId hello bonjour whatever -Pp1 -Dkey1=value1 -Dkey2 -DskipTests=true -DaltDeploymentRepository=runtimes-artifacts::default::REPOSITORY -Denforcer.skip=true | tee $WORKSPACE/LOG_FILE ; test ${PIPESTATUS[0]} -eq 0', returnStdout: false])
    }

    def "[MavenCommand.groovy] withSettingsXmlId"() {
        setup:
        steps.env = ['MAVEN_SETTINGS_XML':'settingsFileId']
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withSettingsXmlId('anyId').run('whatever')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -s settingsFileId whatever', returnStdout: false])
    }

    def "[MavenCommand.groovy] withSettingsXmlId not existing"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withSettingsXmlId('anyId').run('whatever')
        then:
        thrown(AssertionError)
    }

    def "[MavenCommand.groovy] withSettingsXmlFile"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withSettingsXmlFile('FILE').run('whatever')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -s FILE whatever', returnStdout: false])
    }

    def "[MavenCommand.groovy] withSettingsXmlFile empty value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withSettingsXmlFile('').run('whatever')
        then:
        thrown(AssertionError)
    }

    def "[MavenCommand.groovy] withOptions"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withOptions(['opt1', 'opt2']).run('whatever')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B opt1 opt2 whatever', returnStdout: false])
    }

    def "[MavenCommand.groovy] withOptions null"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withOptions(null).run('whatever')
        then:
        thrown(Exception)
    }

    def "[MavenCommand.groovy] skipTests no value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.skipTests().run('whatever')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B whatever -DskipTests=true', returnStdout: false])
    }

    def "[MavenCommand.groovy] skipTests with value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
            .skipTests(false)
        when:
        mvnCommand.run('whatever')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B whatever -DskipTests=false', returnStdout: false])
    }

    def "[MavenCommand.groovy] withProfiles"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withProfiles(['profile1','profile2']).run('whatever')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B whatever -Pprofile1,profile2', returnStdout: false])
    }

    def "[MavenCommand.groovy] withProfiles null"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withProfiles(null).run('whatever')
        then:
        thrown(Exception)
    }

    def "[MavenCommand.groovy] withProperty no value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withProperty('prop').run('whatever')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B whatever -Dprop', returnStdout: false])
    }

    def "[MavenCommand.groovy] withProperty empty value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withProperty('prop', '').run('whatever')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B whatever -Dprop', returnStdout: false])
    }

    def "[MavenCommand.groovy] withProperty with value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withProperty('prop', 'value').run('whatever')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B whatever -Dprop=value', returnStdout: false])
    }

    def "[MavenCommand.groovy] withProperties"() {
        setup:
        def props = new Properties()
        props.put('key1', 'value1')
        props.put('key2', 'value2')
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withProperties(props).run('whatever')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B whatever -Dkey2=value2 -Dkey1=value1', returnStdout: false])
    }

    def "[MavenCommand.groovy] withProperties null value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withProperties(null).run('whatever')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B whatever', returnStdout: false])
    }

    def "[MavenCommand.groovy] withPropertyMap"() {
        setup:
        def props = [
            'key1' : 'value1',
            'key2' : 'value2'
        ]
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withPropertyMap(props).run('whatever')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B whatever -Dkey1=value1 -Dkey2=value2', returnStdout: false])
    }

    def "[MavenCommand.groovy] withPropertyMap null value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withPropertyMap(null).run('whatever')
        then:
        thrown(Exception)
    }

    def "[MavenCommand.groovy] withLogFileName"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withLogFileName('LOGFILE').run('whatever')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B whatever | tee $WORKSPACE/LOGFILE ; test ${PIPESTATUS[0]} -eq 0', returnStdout: false])
    }

    def "[MavenCommand.groovy] withLogFileName empty value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withLogFileName('').run('whatever')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B whatever', returnStdout: false])
    }

    def "[MavenCommand.groovy] withDeployRepository"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withDeployRepository('REPOSITORY').run('whatever')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B whatever -DaltDeploymentRepository=runtimes-artifacts::default::REPOSITORY -Denforcer.skip=true', returnStdout: false])
    }

    def "[MavenCommand.groovy] withDeployRepository empty value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withDeployRepository('').run('whatever')
        then:
        thrown(AssertionError)
    }

    def "[MavenCommand.groovy] withLocalDeployFolder"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withLocalDeployFolder('LOCAL_FOLDER').run('whatever')
        then:         
        1 * getPipelineMock("sh")([script: 'mvn -B whatever -DaltDeploymentRepository=local::default::file://LOCAL_FOLDER', returnStdout: false])
    }

    def "[MavenCommand.groovy] withLocalDeployFolder empty value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withLocalDeployFolder('').run('whatever')
        then:
        thrown(AssertionError)
    }

    def "[MavenCommand.groovy] clone ok"() {
        setup:
        steps.env = ['MAVEN_SETTINGS_XML':'settingsFileId']
        def props = new Properties()
        props.put('key1', 'value1')
        def mvnCommand = new MavenCommand(steps)
            .withProperties(props)
            .withProperty('key2')
            .withSettingsXmlId('anyId')
            .withOptions(['hello', 'bonjour'])
            .withProfiles(['p1'])
            .skipTests(true)
            .withLocalDeployFolder('LOCAL_FOLDER')
        when:
        mvnCommand.run('clean deploy')
        def newCmd = mvnCommand
            .clone()
            .withProperty('key3', 'value3')
            .withLogFileName('LOG_FILE')
            .withProfiles(['p2'])
            .withDeployRepository('REPOSITORY')
        newCmd.run('clean deploy')
        mvnCommand.run('clean deploy')
        then:
        2 * getPipelineMock("sh")([script: 'mvn -B -s settingsFileId hello bonjour clean deploy -Pp1 -Dkey1=value1 -Dkey2 -DskipTests=true -DaltDeploymentRepository=local::default::file://LOCAL_FOLDER', returnStdout: false])
        1 * getPipelineMock("sh")([script: 'mvn -B -s settingsFileId hello bonjour clean deploy -Pp1,p2 -Dkey1=value1 -Dkey2 -DskipTests=true -DaltDeploymentRepository=runtimes-artifacts::default::REPOSITORY -Dkey3=value3 -Denforcer.skip=true | tee $WORKSPACE/LOG_FILE ; test ${PIPESTATUS[0]} -eq 0', returnStdout: false])
    }

    def "[MavenCommand.groovy] returnOutput"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        def output = mvnCommand.returnOutput().run('whatever')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B whatever', returnStdout: true])>> { return 'This is output' }
        'This is output' == output 
    }
}
