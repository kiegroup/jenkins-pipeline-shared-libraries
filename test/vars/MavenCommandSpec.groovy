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
        1 * getPipelineMock("sh")('mvn -B whatever')
    }

    def "[MavenCommand.groovy] run with all set"() {
        setup:
        steps.env = ['MAVEN_SETTINGS_XML':'settingsFileId']
        def logfile = 'LOG_FILE'
        def props = new Properties()
        props.put('key1', 'value1')
        def mvnCommand = new MavenCommand(steps)
            .withProperties(props)
            .withProperty('key2')
            .withSettingsXmlId('anyId')
            .withOptions(['hello', 'bonjour'])
            .withProfiles(['p1'])
            .skipTests(true)
            .withLogFileName(logfile)
        when:
        mvnCommand.run('whatever')
        then:
        1 * getPipelineMock("sh")('mvn -B -s settingsFileId hello bonjour whatever -Pp1 -Dkey1=value1 -Dkey2 -DskipTests=true | tee $WORKSPACE/LOG_FILE ; test ${PIPESTATUS[0]} -eq 0')
    }

    def "[MavenCommand.groovy] clean"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.runClean()
        then:
        1 * getPipelineMock("sh")('mvn -B clean')
    }

    def "[MavenCommand.groovy] package"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.runPackage()
        then:
        1 * getPipelineMock("sh")('mvn -B package')
    }

    def "[MavenCommand.groovy] clean package"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.runPackage(true)
        then:
        1 * getPipelineMock("sh")('mvn -B clean package')
    }

    def "[MavenCommand.groovy] install"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.runInstall()
        then:
        1 * getPipelineMock("sh")('mvn -B install')
    }

    def "[MavenCommand.groovy] clean install"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.runInstall(true)
        then:
        1 * getPipelineMock("sh")('mvn -B clean install')
    }

    def "[MavenCommand.groovy] verify"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.runVerify()
        then:
        1 * getPipelineMock("sh")('mvn -B verify')
    }

    def "[MavenCommand.groovy] clean verify"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.runVerify(true)
        then:
        1 * getPipelineMock("sh")('mvn -B clean verify')
    }

    def "[MavenCommand.groovy] deploy"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.runDeploy()
        then:
        1 * getPipelineMock("sh")('mvn -B deploy')
    }

    def "[MavenCommand.groovy] clean deploy"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.runDeploy(true)
        then:
        1 * getPipelineMock("sh")('mvn -B clean deploy')
    }

    def "[MavenCommand.groovy] deploy with deploy repo"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.runDeploy(false, 'DEPLOY_REPO')
        then:
        1 * getPipelineMock("sh")('mvn -B deploy -DaltDeploymentRepository=runtimes-artifacts::default::DEPLOY_REPO -Denforcer.skip=true')
    }

    def "[MavenCommand.groovy] clean deploy with deploy repo"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.runDeploy(true, 'DEPLOY_REPO')
        then:
        1 * getPipelineMock("sh")('mvn -B clean deploy -DaltDeploymentRepository=runtimes-artifacts::default::DEPLOY_REPO -Denforcer.skip=true')
    }

        def "[MavenCommand.groovy] deployLocally"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.runDeployLocally('LOCAL_FOLDER')
        then:
        1 * getPipelineMock("sh")('mvn -B deploy -DaltDeploymentRepository=local::default::file://LOCAL_FOLDER')
    }

    def "[MavenCommand.groovy] clean deployLocally"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.runDeployLocally('LOCAL_FOLDER', true)
        then:
        1 * getPipelineMock("sh")('mvn -B clean deploy -DaltDeploymentRepository=local::default::file://LOCAL_FOLDER')
    }

    def "[MavenCommand.groovy] withSettingsXmlId"() {
        setup:
        steps.env = ['MAVEN_SETTINGS_XML':'settingsFileId']
        def mvnCommand = new MavenCommand(steps)
            .withSettingsXmlId('anyId')
        when:
        mvnCommand.run('whatever')
        then:
        1 * getPipelineMock("sh")('mvn -B -s settingsFileId whatever')
    }

    def "[MavenCommand.groovy] withSettingsXmlFile"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
            .withSettingsXmlFile('FILE')
        when:
        mvnCommand.run('whatever')
        then:
        1 * getPipelineMock("sh")('mvn -B -s FILE whatever')
    }

    def "[MavenCommand.groovy] withOptions"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
            .withOptions(['opt1', 'opt2'])
        when:
        mvnCommand.run('whatever')
        then:
        1 * getPipelineMock("sh")('mvn -B opt1 opt2 whatever')
    }

    def "[MavenCommand.groovy] skipTests no value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
            .skipTests()
        when:
        mvnCommand.run('whatever')
        then:
        1 * getPipelineMock("sh")('mvn -B whatever -DskipTests=true')
    }

    def "[MavenCommand.groovy] skipTests with value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
            .skipTests(false)
        when:
        mvnCommand.run('whatever')
        then:
        1 * getPipelineMock("sh")('mvn -B whatever -DskipTests=false')
    }

    def "[MavenCommand.groovy] withProfiles"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
            .withProfiles(['profile1','profile2'])
        when:
        mvnCommand.run('whatever')
        then:
        1 * getPipelineMock("sh")('mvn -B whatever -Pprofile1,profile2')
    }

    def "[MavenCommand.groovy] withProperty no value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
            .withProperty('prop')
        when:
        mvnCommand.run('whatever')
        then:
        1 * getPipelineMock("sh")('mvn -B whatever -Dprop')
    }

    def "[MavenCommand.groovy] withProperty empty value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
            .withProperty('prop', '')
        when:
        mvnCommand.run('whatever')
        then:
        1 * getPipelineMock("sh")('mvn -B whatever -Dprop')
    }

    def "[MavenCommand.groovy] withProperty with value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
            .withProperty('prop', 'value')
        when:
        mvnCommand.run('whatever')
        then:
        1 * getPipelineMock("sh")('mvn -B whatever -Dprop=value')
    }

    def "[MavenCommand.groovy] withProperties"() {
        setup:
        def props = new Properties()
        props.put('key1', 'value1')
        props.put('key2', 'value2')
        def mvnCommand = new MavenCommand(steps)
            .withProperties(props)
        when:
        mvnCommand.run('whatever')
        then:
        1 * getPipelineMock("sh")('mvn -B whatever -Dkey2=value2 -Dkey1=value1')
    }

    def "[MavenCommand.groovy] withProperties null value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
            .withProperties(null)
        when:
        mvnCommand.run('whatever')
        then:
        1 * getPipelineMock("sh")('mvn -B whatever')
    }

    def "[MavenCommand.groovy] withPropertyMap"() {
        setup:
        def props = [
            'key1' : 'value1',
            'key2' : 'value2'
        ]
        def mvnCommand = new MavenCommand(steps)
            .withPropertyMap(props)
        when:
        mvnCommand.run('whatever')
        then:
        1 * getPipelineMock("sh")('mvn -B whatever -Dkey1=value1 -Dkey2=value2')
    }

    def "[MavenCommand.groovy] withPropertyMap null value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
            .withPropertyMap(null)
        when:
        mvnCommand.run('whatever')
        then:
        1 * getPipelineMock("sh")('mvn -B whatever')
    }

    def "[MavenCommand.groovy] withLogFileName"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
            .withLogFileName('LOGFILE')
        when:
        mvnCommand.run('whatever')
        then:
        1 * getPipelineMock("sh")('mvn -B whatever | tee $WORKSPACE/LOGFILE ; test ${PIPESTATUS[0]} -eq 0')
    }


    def "[MavenCommand.groovy] clone ok"() {
        setup:
        steps.env = ['MAVEN_SETTINGS_XML':'settingsFileId']
        def logfile = 'LOG_FILE'
        def props = new Properties()
        props.put('key1', 'value1')
        def mvnCommand = new MavenCommand(steps)
            .withProperties(props)
            .withProperty('key2')
            .withSettingsXmlId('anyId')
            .withOptions(['hello', 'bonjour'])
            .withProfiles(['p1'])
            .skipTests(true)
        when:
        mvnCommand.run('clean')
        def newCmd = mvnCommand
            .clone()
            .withProperty('key3', 'value3')
            .withLogFileName(logfile)
            .withProfiles(['p2'])
        newCmd.run('clean')
        mvnCommand.run('clean')
        then:
        2 * getPipelineMock("sh")('mvn -B -s settingsFileId hello bonjour clean -Pp1 -Dkey1=value1 -Dkey2 -DskipTests=true')
        1 * getPipelineMock("sh")('mvn -B -s settingsFileId hello bonjour clean -Pp1,p2 -Dkey1=value1 -Dkey2 -DskipTests=true -Dkey3=value3 | tee $WORKSPACE/LOG_FILE ; test ${PIPESTATUS[0]} -eq 0')
    }

    def "[MavenCommand.groovy] returnOutput"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
            .returnOutput()
        when:
        def output = mvnCommand.run('whatever')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B whatever', returnStdout: true])>> { return 'This is output' }
        'This is output' == output 
    }
}
