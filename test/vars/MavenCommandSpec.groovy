import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import org.jenkinsci.plugins.workflow.steps.Step
import org.jenkinsci.plugins.workflow.steps.StepContext
import org.jenkinsci.plugins.workflow.steps.StepExecution
import org.kie.jenkins.MavenCommand

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
        1 * getPipelineMock('sh')([script: 'mvn -B whatever', returnStdout: false])
        0 * getPipelineMock('error')(_)
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
            .withDependencyRepositoryInSettings('ID', 'URL')
            .withMirrorDisabledForRepoInSettings('DISABLED_ID')
            .withSnapshotsDisabledInSettings()
            .withSettingsXmlId('anyId')
            .withOptions(['hello', 'bonjour'])
            .withProfiles(['p1'])
            .skipTests(true)
            .withLogFileName('LOG_FILE')
            .withDeployRepository('REPOSITORY')
            .useMavenWrapper()
            .run('whatever')
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: 'mktemp --suffix -settings.xml']) >> 'anything-settings.xml'
        1 * getPipelineMock("sh").call(['script':'./mnw -B -s settingsFileId hello bonjour whatever -Pp1 -Dkey1=value1 -Dkey2 -Denforcer.skip=true -DskipTests=true -DaltDeploymentRepository=runtimes-artifacts::default::REPOSITORY | tee $WORKSPACE/LOG_FILE ; test ${PIPESTATUS[0]} -eq 0', 'returnStdout':false])
        1 * getPipelineMock('sh')("""
            sed -i 's|<repositories>|<repositories><!-- BEGIN added repository --><repository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></repository><!-- END added repository -->|g' settingsFileId
            sed -i 's|<pluginRepositories>|<pluginRepositories><!-- BEGIN added repository --><pluginRepository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></pluginRepository><!-- END added repository -->|g' settingsFileId
        """)
        1 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!ID</mirrorOf>|g' settingsFileId")
        1 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!DISABLED_ID</mirrorOf>|g' settingsFileId")
        1 * getPipelineMock('sh')("sed -i '/<repository>/,/<\\/repository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' settingsFileId")
        1 * getPipelineMock('sh')("sed -i '/<pluginRepository>/,/<\\/pluginRepository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' settingsFileId")
    }

    def "[MavenCommand.groovy] inDirectory"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.inDirectory('DIR').run('whatever')
        then:
        1 * getPipelineMock('dir')(_)
        1 * getPipelineMock('sh')([script: 'mvn -B whatever', returnStdout: false])
    }

    def "[MavenCommand.groovy] inDirectory empty"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.inDirectory('').run('whatever')
        then:
        0 * getPipelineMock('dir')(_)
        1 * getPipelineMock('sh')([script: 'mvn -B whatever', returnStdout: false])
    }

    def "[MavenCommand.groovy] inDirectory with output"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        def output = mvnCommand.inDirectory('').returnOutput().run('whatever')
        then:
        0 * getPipelineMock('dir')(_)
        1 * getPipelineMock('sh')([script: 'mvn -B whatever', returnStdout: true]) >> 'output'
        output.length() > 0
    }

    def "[MavenCommand.groovy] useMavenWrapper"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.useMavenWrapper().run('whatever')
        then:
        1 * getPipelineMock("sh").call(['script':'./mnw -B whatever', 'returnStdout':false])
    }

    def "[MavenCommand.groovy] withSettingsXmlId"() {
        setup:
        steps.env = ['MAVEN_SETTINGS_XML':'settingsFileId']
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withSettingsXmlId('anyId').run('whatever')
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: 'mktemp --suffix -settings.xml']) >> 'anything-settings.xml'
        1 * getPipelineMock('sh')([script: 'mvn -B -s settingsFileId whatever', returnStdout: false])
    }

    def "[MavenCommand.groovy] withSettingsXmlId not existing"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withSettingsXmlId('anyId').run('whatever')
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: 'mktemp --suffix -settings.xml']) >> 'anything-settings.xml'
        0 * getPipelineMock('sh')([script: 'mvn -B -s settingsFileId whatever', returnStdout: false])
    }

    def "[MavenCommand.groovy] withSettingsXmlFile"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withSettingsXmlFile('FILE').run('whatever')
        then:
        1 * getPipelineMock('sh')([script: 'mvn -B -s FILE whatever', returnStdout: false])
    }

    def "[MavenCommand.groovy] withSettingsXmlFile empty value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withSettingsXmlFile('').run('whatever')
        then:
        thrown(AssertionError)
    }

    def "[MavenCommand.groovy] withSettingsXmlFile and withSettingsXmlId"() {
        setup:
        steps.env = ['MAVEN_SETTINGS_XML':'settingsFileId']
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withSettingsXmlFile('FILE').withSettingsXmlId('anyId').run('whatever')
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: 'mktemp --suffix -settings.xml']) >> 'anything-settings.xml'
        0 * getPipelineMock('sh')([script: 'mvn -B -s FILE whatever', returnStdout: false])
        1 * getPipelineMock('sh')([script: 'mvn -B -s settingsFileId whatever', returnStdout: false])
    }

    def "[MavenCommand.groovy] withDependencyRepositoryInSettings"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withSettingsXmlFile('FILE').withDependencyRepositoryInSettings('ID', 'URL').run('whatever')
        then:
        1 * getPipelineMock('sh')("""
            sed -i 's|<repositories>|<repositories><!-- BEGIN added repository --><repository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></repository><!-- END added repository -->|g' FILE
            sed -i 's|<pluginRepositories>|<pluginRepositories><!-- BEGIN added repository --><pluginRepository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></pluginRepository><!-- END added repository -->|g' FILE
        """)
        1 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!ID</mirrorOf>|g' FILE")
        1 * getPipelineMock('sh')([script: 'mvn -B -s FILE whatever -Denforcer.skip=true', returnStdout: false])
    }

    def "[MavenCommand.groovy] withMirrorDisabledForRepoInSettings"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withSettingsXmlFile('FILE').withMirrorDisabledForRepoInSettings('ID').run('whatever')
        then:
        1 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!ID</mirrorOf>|g' FILE")
        1 * getPipelineMock('sh')([script: 'mvn -B -s FILE whatever', returnStdout: false])
    }

    def "[MavenCommand.groovy] withSnapshotsDisabledInSettings"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withSettingsXmlFile('FILE').withSnapshotsDisabledInSettings().run('whatever')
        then:
        1 * getPipelineMock('sh')("sed -i '/<repository>/,/<\\/repository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' FILE")
        1 * getPipelineMock('sh')("sed -i '/<pluginRepository>/,/<\\/pluginRepository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' FILE")
    }

    def "[MavenCommand.groovy] withOptions"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withOptions(['opt1', 'opt2']).run('whatever')
        then:
        1 * getPipelineMock('sh')([script: 'mvn -B opt1 opt2 whatever', returnStdout: false])
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
        1 * getPipelineMock('sh')([script: 'mvn -B whatever -DskipTests=true', returnStdout: false])
    }

    def "[MavenCommand.groovy] skipTests with value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
            .skipTests(false)
        when:
        mvnCommand.run('whatever')
        then:
        1 * getPipelineMock('sh')([script: 'mvn -B whatever -DskipTests=false', returnStdout: false])
    }

    def "[MavenCommand.groovy] withProfiles"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withProfiles(['profile1', 'profile2']).run('whatever')
        then:
        1 * getPipelineMock('sh')([script: 'mvn -B whatever -Pprofile1,profile2', returnStdout: false])
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
        1 * getPipelineMock('sh')([script: 'mvn -B whatever -Dprop', returnStdout: false])
    }

    def "[MavenCommand.groovy] withProperty empty value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withProperty('prop', '').run('whatever')
        then:
        1 * getPipelineMock('sh')([script: 'mvn -B whatever -Dprop', returnStdout: false])
    }

    def "[MavenCommand.groovy] withProperty with value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withProperty('prop', 'value').run('whatever')
        then:
        1 * getPipelineMock('sh')([script: 'mvn -B whatever -Dprop=value', returnStdout: false])
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
        1 * getPipelineMock('sh')([script: 'mvn -B whatever -Dkey2=value2 -Dkey1=value1', returnStdout: false])
    }

    def "[MavenCommand.groovy] withProperties null value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withProperties(null).run('whatever')
        then:
        1 * getPipelineMock('sh')([script: 'mvn -B whatever', returnStdout: false])
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
        1 * getPipelineMock('sh')([script: 'mvn -B whatever -Dkey1=value1 -Dkey2=value2', returnStdout: false])
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
        1 * getPipelineMock('sh')([script: 'mvn -B whatever | tee $WORKSPACE/LOGFILE ; test ${PIPESTATUS[0]} -eq 0', returnStdout: false])
    }

    def "[MavenCommand.groovy] withLogFileName empty value"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withLogFileName('').run('whatever')
        then:
        1 * getPipelineMock('sh')([script: 'mvn -B whatever', returnStdout: false])
    }

    def "[MavenCommand.groovy] withDeployRepository"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        mvnCommand.withDeployRepository('REPOSITORY').run('whatever')
        then:
        1 * getPipelineMock('sh')([script: 'mvn -B whatever -DaltDeploymentRepository=runtimes-artifacts::default::REPOSITORY -Denforcer.skip=true', returnStdout: false])
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
        1 * getPipelineMock('sh')([script: 'mvn -B whatever -DaltDeploymentRepository=local::default::file://LOCAL_FOLDER', returnStdout: false])
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
            .withSettingsXmlFile('SETTINGS_FILE')
            .withOptions(['hello', 'bonjour'])
            .withProfiles(['p1'])
            .skipTests(true)
            .withLocalDeployFolder('LOCAL_FOLDER')
            .withDependencyRepositoryInSettings('ID','URL')
            .withMirrorDisabledForRepoInSettings('DISABLED_ID')
            .withSnapshotsDisabledInSettings()
        when:
        mvnCommand.run('clean deploy')
        def newCmd = mvnCommand
            .clone()
            .withProperty('key3', 'value3')
            .withSettingsXmlId('anyId')
            .withLogFileName('LOG_FILE')
            .withProfiles(['p2'])
            .withDeployRepository('REPOSITORY')
        newCmd.run('clean deploy')
        mvnCommand.run('clean deploy')
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: 'mktemp --suffix -settings.xml']) >> 'anything-settings.xml'
        2 * getPipelineMock('sh')([script: 'mvn -B -s SETTINGS_FILE hello bonjour clean deploy -Pp1 -Dkey1=value1 -Dkey2 -DskipTests=true -DaltDeploymentRepository=local::default::file://LOCAL_FOLDER -Denforcer.skip=true', returnStdout: false])
        2 * getPipelineMock('sh')("""
            sed -i 's|<repositories>|<repositories><!-- BEGIN added repository --><repository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></repository><!-- END added repository -->|g' SETTINGS_FILE
            sed -i 's|<pluginRepositories>|<pluginRepositories><!-- BEGIN added repository --><pluginRepository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></pluginRepository><!-- END added repository -->|g' SETTINGS_FILE
        """)
        2 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!ID</mirrorOf>|g' SETTINGS_FILE")
        2 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!DISABLED_ID</mirrorOf>|g' SETTINGS_FILE")
        2 * getPipelineMock('sh')("sed -i '/<repository>/,/<\\/repository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' SETTINGS_FILE")
        2 * getPipelineMock('sh')("sed -i '/<pluginRepository>/,/<\\/pluginRepository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' SETTINGS_FILE")
        1 * getPipelineMock('sh')([script: 'mvn -B -s settingsFileId hello bonjour clean deploy -Pp1,p2 -Dkey1=value1 -Dkey2 -DskipTests=true -DaltDeploymentRepository=runtimes-artifacts::default::REPOSITORY -Denforcer.skip=true -Dkey3=value3 | tee $WORKSPACE/LOG_FILE ; test ${PIPESTATUS[0]} -eq 0', returnStdout: false])
        1 * getPipelineMock('sh')("""
            sed -i 's|<repositories>|<repositories><!-- BEGIN added repository --><repository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></repository><!-- END added repository -->|g' settingsFileId
            sed -i 's|<pluginRepositories>|<pluginRepositories><!-- BEGIN added repository --><pluginRepository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></pluginRepository><!-- END added repository -->|g' settingsFileId
        """)
        1 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!ID</mirrorOf>|g' settingsFileId")
        1 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!DISABLED_ID</mirrorOf>|g' settingsFileId")
        1 * getPipelineMock('sh')("sed -i '/<repository>/,/<\\/repository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' settingsFileId")
        1 * getPipelineMock('sh')("sed -i '/<pluginRepository>/,/<\\/pluginRepository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' settingsFileId")
    }

    def "[MavenCommand.groovy] returnOutput"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        def output = mvnCommand.returnOutput().run('whatever')
        then:
        1 * getPipelineMock('sh')([script: 'mvn -B whatever', returnStdout: true]) >> { return 'This is output' }
        'This is output' == output
    }

    def "[MavenCommand.groovy] returnOutput false"() {
        setup:
        def mvnCommand = new MavenCommand(steps)
        when:
        def output = mvnCommand.returnOutput(false).run('whatever')
        then:
        1 * getPipelineMock('sh')([script: 'mvn -B whatever', returnStdout: false])
    }

    def "[MavenCommand.groovy] printSettings"() {
        setup:
        def mvnCommand = new MavenCommand(steps).withSettingsXmlFile('SETTINGS_FILE')
        when:
        mvnCommand.printSettings().run('whatever')
        then:
        1 * getPipelineMock('sh')('cat SETTINGS_FILE')
    }

    def "[MavenCommand.groovy] printSettings false"() {
        setup:
        def mvnCommand = new MavenCommand(steps).withSettingsXmlFile('SETTINGS_FILE')
        when:
        mvnCommand.printSettings(false).run('whatever')
        then:
        0 * getPipelineMock('sh')('cat SETTINGS_FILE')
    }

}
