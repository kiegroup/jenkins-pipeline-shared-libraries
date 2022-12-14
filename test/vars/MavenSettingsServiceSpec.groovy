import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import org.jenkinsci.plugins.workflow.steps.Step
import org.jenkinsci.plugins.workflow.steps.StepContext
import org.jenkinsci.plugins.workflow.steps.StepExecution
import org.kie.jenkins.MavenSettingsConfigBuilder
import org.kie.jenkins.MavenSettingsService

class MavenSettingsServiceSpec extends JenkinsPipelineSpecification {

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

    def "[MavenSettingsService.groovy] create with all set"() {
        setup:
        steps.env = ['MAVEN_SETTINGS_XML':'settingsFileId']
        def mvnCfg = new MavenSettingsConfigBuilder()
            .dependenciesRepositoriesInSettings([ID: 'URL'])
            .disabledMirrorRepoInSettings(['DISABLED_ID'] as Set)
            .disableSnapshotsInSettings(true)
            .settingsXmlConfigFileId('anyId')
            .build()
        when:
        new MavenSettingsService(steps, mvnCfg).createSettingsFile()
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: 'mktemp --suffix -settings.xml']) >> 'anything-settings.xml'
        1 * getPipelineMock('sh')("""
            sed -i 's|<repositories>|<repositories><!-- BEGIN added repository --><repository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></repository><!-- END added repository -->|g' settingsFileId
            sed -i 's|<pluginRepositories>|<pluginRepositories><!-- BEGIN added repository --><pluginRepository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></pluginRepository><!-- END added repository -->|g' settingsFileId
        """)
        1 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!ID</mirrorOf>|g' settingsFileId")
        1 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!DISABLED_ID</mirrorOf>|g' settingsFileId")
        1 * getPipelineMock('sh')("sed -i '/<repository>/,/<\\/repository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' settingsFileId")
        1 * getPipelineMock('sh')("sed -i '/<pluginRepository>/,/<\\/pluginRepository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' settingsFileId")
    }

    def "[MavenSettingsService.groovy] settingsXmlConfigFileId"() {
        setup:
        steps.env = ['MAVEN_SETTINGS_XML':'settingsFileId']
        def mvnCfg = new MavenSettingsConfigBuilder()
            .settingsXmlConfigFileId('anyId')
            .build()
        when:
        def settingsFile = new MavenSettingsService(steps, mvnCfg).createSettingsFile()
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: 'mktemp --suffix -settings.xml']) >> 'anything-settings.xml'
        settingsFile == 'settingsFileId'
    }

    def "[MavenSettingsService.groovy] settingsXmlConfigFileId not existing"() {
        setup:
        def mvnCfg = new MavenSettingsConfigBuilder()
            .settingsXmlConfigFileId('anyId')
            .build()
        when:
        def settingsFile = new MavenSettingsService(steps, mvnCfg).createSettingsFile()
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: 'mktemp --suffix -settings.xml']) >> 'anything-settings.xml'
        settingsFile == ''
    }

    def "[MavenSettingsService.groovy] settingsXmlPath"() {
        setup:
        def mvnCfg = new MavenSettingsConfigBuilder() 
            .settingsXmlPath('FILE')
            .build()
        when:
        def settingsFile = new MavenSettingsService(steps, mvnCfg).createSettingsFile()
        then:
        settingsFile == 'FILE'
    }

    def "[MavenSettingsService.groovy] settingsXmlPath and settingsXmlConfigFileId"() {
        setup:
        steps.env = ['MAVEN_SETTINGS_XML':'settingsFileId']
        def mvnCfg = new MavenSettingsConfigBuilder()
            .settingsXmlPath('FILE')
            .settingsXmlConfigFileId('anyId')
            .build()
        when:
        def settingsFile = new MavenSettingsService(steps, mvnCfg).createSettingsFile()
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: 'mktemp --suffix -settings.xml']) >> 'anything-settings.xml'
        settingsFile == 'settingsFileId'
    }

    def "[MavenSettingsService.groovy] dependenciesRepositoriesInSettings"() {
        setup:
        def mvnCfg = new MavenSettingsConfigBuilder()
            .settingsXmlPath('FILE')
            .dependenciesRepositoriesInSettings([ID: 'URL'])
            .build()
        when:
        def settingsFile = new MavenSettingsService(steps, mvnCfg).createSettingsFile()
        then:
        1 * getPipelineMock('sh')("""
            sed -i 's|<repositories>|<repositories><!-- BEGIN added repository --><repository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></repository><!-- END added repository -->|g' FILE
            sed -i 's|<pluginRepositories>|<pluginRepositories><!-- BEGIN added repository --><pluginRepository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></pluginRepository><!-- END added repository -->|g' FILE
        """)
        1 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!ID</mirrorOf>|g' FILE")
        settingsFile == 'FILE'
    }

    def "[MavenSettingsService.groovy] disabledMirrorRepoInSettings"() {
        setup:
        def mvnCfg = new MavenSettingsConfigBuilder()
            .settingsXmlPath('FILE')
            .disabledMirrorRepoInSettings(['ID'] as Set)
            .build()
        when:
        def settingsFile = new MavenSettingsService(steps, mvnCfg).createSettingsFile()
        then:
        1 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!ID</mirrorOf>|g' FILE")
        settingsFile == 'FILE'
    }

    def "[MavenSettingsService.groovy] disableSnapshotsInSettings"() {
        setup:
        def mvnCfg = new MavenSettingsConfigBuilder()
            .settingsXmlPath('FILE')
            .disableSnapshotsInSettings(true)
            .build()
        when:
        def settingsFile = new MavenSettingsService(steps, mvnCfg).createSettingsFile()
        then:
        1 * getPipelineMock('sh')("sed -i '/<repository>/,/<\\/repository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' FILE")
        1 * getPipelineMock('sh')("sed -i '/<pluginRepository>/,/<\\/pluginRepository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' FILE")
        settingsFile == 'FILE'
    }

    def "[MavenSettingsService.groovy] clone ok"() {
        setup:
        steps.env = ['MAVEN_SETTINGS_XML':'settingsFileId']
        def props = new Properties()
        props.put('key1', 'value1')
        def mvnCfg = new MavenSettingsConfigBuilder()
            .settingsXmlPath('SETTINGS_FILE')
            .dependenciesRepositoriesInSettings([ID: 'URL'])
            .disabledMirrorRepoInSettings(['DISABLED_ID'] as Set)
            .disableSnapshotsInSettings(true)
            .build()
        when:
        new MavenSettingsService(steps, mvnCfg).createSettingsFile()
        def newCfg = MavenSettingsConfigBuilder.from(mvnCfg)
            .settingsXmlConfigFileId('anyId')
            .build()
        def newSettingsFile = new MavenSettingsService(steps, newCfg).createSettingsFile()
        def settingsFile = new MavenSettingsService(steps, mvnCfg).createSettingsFile()
        then:
        1 * getPipelineMock("sh")([returnStdout: true, script: 'mktemp --suffix -settings.xml']) >> 'anything-settings.xml'
        2 * getPipelineMock('sh')("""
            sed -i 's|<repositories>|<repositories><!-- BEGIN added repository --><repository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></repository><!-- END added repository -->|g' SETTINGS_FILE
            sed -i 's|<pluginRepositories>|<pluginRepositories><!-- BEGIN added repository --><pluginRepository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></pluginRepository><!-- END added repository -->|g' SETTINGS_FILE
        """)
        2 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!ID</mirrorOf>|g' SETTINGS_FILE")
        2 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!DISABLED_ID</mirrorOf>|g' SETTINGS_FILE")
        2 * getPipelineMock('sh')("sed -i '/<repository>/,/<\\/repository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' SETTINGS_FILE")
        2 * getPipelineMock('sh')("sed -i '/<pluginRepository>/,/<\\/pluginRepository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' SETTINGS_FILE")
        1 * getPipelineMock('sh')("""
            sed -i 's|<repositories>|<repositories><!-- BEGIN added repository --><repository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></repository><!-- END added repository -->|g' settingsFileId
            sed -i 's|<pluginRepositories>|<pluginRepositories><!-- BEGIN added repository --><pluginRepository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></pluginRepository><!-- END added repository -->|g' settingsFileId
        """)
        1 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!ID</mirrorOf>|g' settingsFileId")
        1 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!DISABLED_ID</mirrorOf>|g' settingsFileId")
        1 * getPipelineMock('sh')("sed -i '/<repository>/,/<\\/repository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' settingsFileId")
        1 * getPipelineMock('sh')("sed -i '/<pluginRepository>/,/<\\/pluginRepository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' settingsFileId")
        settingsFile == 'SETTINGS_FILE'
        newSettingsFile == 'settingsFileId'
    }

}
