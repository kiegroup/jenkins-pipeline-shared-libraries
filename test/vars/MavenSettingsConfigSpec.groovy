import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import org.jenkinsci.plugins.workflow.steps.Step
import org.jenkinsci.plugins.workflow.steps.StepContext
import org.jenkinsci.plugins.workflow.steps.StepExecution
import org.kie.jenkins.MavenSettingsConfig

class MavenSettingsConfigSpec extends JenkinsPipelineSpecification {

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

    def "[MavenSettingsConfig.groovy] create with all set"() {
        setup:
        steps.env = ['MAVEN_SETTINGS_XML':'settingsFileId']
        def mvnCfg = new MavenSettingsConfig(steps)
        when:
        mvnCfg
            .withDependencyRepositoryInSettings('ID', 'URL')
            .withMirrorDisabledForRepoInSettings('DISABLED_ID')
            .withSnapshotsDisabledInSettings()
            .withSettingsXmlId('anyId')
            .createSettingsFile()
        then:
        1 * getPipelineMock('sh')("""
            sed -i 's|<repositories>|<repositories><!-- BEGIN added repository --><repository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></repository><!-- END added repository -->|g' settingsFileId
            sed -i 's|<pluginRepositories>|<pluginRepositories><!-- BEGIN added repository --><pluginRepository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></pluginRepository><!-- END added repository -->|g' settingsFileId
        """)
        1 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!ID</mirrorOf>|g' settingsFileId")
        1 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!DISABLED_ID</mirrorOf>|g' settingsFileId")
        1 * getPipelineMock('sh')("sed -i '/<repository>/,/<\\/repository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' settingsFileId")
        1 * getPipelineMock('sh')("sed -i '/<pluginRepository>/,/<\\/pluginRepository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' settingsFileId")
    }

    def "[MavenSettingsConfig.groovy] withSettingsXmlId"() {
        setup:
        steps.env = ['MAVEN_SETTINGS_XML':'settingsFileId']
        def mvnCfg = new MavenSettingsConfig(steps)
        when:
        def settingsFile = mvnCfg.withSettingsXmlId('anyId').createSettingsFile()
        then:
        settingsFile == 'settingsFileId'
    }

    def "[MavenSettingsConfig.groovy] withSettingsXmlId not existing"() {
        setup:
        def mvnCfg = new MavenSettingsConfig(steps)
        when:
        def settingsFile = mvnCfg.withSettingsXmlId('anyId').createSettingsFile()
        then:
        settingsFile == ''
    }

    def "[MavenSettingsConfig.groovy] withSettingsXmlFile"() {
        setup:
        def mvnCfg = new MavenSettingsConfig(steps)
        when:
        def settingsFile = mvnCfg.withSettingsXmlFile('FILE').createSettingsFile()
        then:
        settingsFile == 'FILE'
    }

    def "[MavenSettingsConfig.groovy] withSettingsXmlFile empty value"() {
        setup:
        def mvnCfg = new MavenSettingsConfig(steps)
        when:
        def settingsFile = mvnCfg.withSettingsXmlFile('').createSettingsFile()
        then:
        thrown(AssertionError)
    }

    def "[MavenSettingsConfig.groovy] withSettingsXmlFile and withSettingsXmlId"() {
        setup:
        steps.env = ['MAVEN_SETTINGS_XML':'settingsFileId']
        def mvnCfg = new MavenSettingsConfig(steps)
        when:
        def settingsFile = mvnCfg.withSettingsXmlFile('FILE').withSettingsXmlId('anyId').createSettingsFile()
        then:
        settingsFile == 'settingsFileId'
    }

    def "[MavenSettingsConfig.groovy] withDependencyRepositoryInSettings"() {
        setup:
        def mvnCfg = new MavenSettingsConfig(steps)
        when:
        def settingsFile = mvnCfg.withSettingsXmlFile('FILE').withDependencyRepositoryInSettings('ID', 'URL').createSettingsFile()
        then:
        1 * getPipelineMock('sh')("""
            sed -i 's|<repositories>|<repositories><!-- BEGIN added repository --><repository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></repository><!-- END added repository -->|g' FILE
            sed -i 's|<pluginRepositories>|<pluginRepositories><!-- BEGIN added repository --><pluginRepository><id>ID</id><name>ID</name><url>URL</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases></pluginRepository><!-- END added repository -->|g' FILE
        """)
        1 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!ID</mirrorOf>|g' FILE")
        settingsFile == 'FILE'
    }

    def "[MavenSettingsConfig.groovy] withMirrorDisabledForRepoInSettings"() {
        setup:
        def mvnCfg = new MavenSettingsConfig(steps)
        when:
        def settingsFile = mvnCfg.withSettingsXmlFile('FILE').withMirrorDisabledForRepoInSettings('ID').createSettingsFile()
        then:
        1 * getPipelineMock('sh')("sed -i 's|</mirrorOf>|,!ID</mirrorOf>|g' FILE")
        settingsFile == 'FILE'
    }

    def "[MavenSettingsConfig.groovy] withSnapshotsDisabledInSettings"() {
        setup:
        def mvnCfg = new MavenSettingsConfig(steps)
        when:
        def settingsFile = mvnCfg.withSettingsXmlFile('FILE').withSnapshotsDisabledInSettings().createSettingsFile()
        then:
        1 * getPipelineMock('sh')("sed -i '/<repository>/,/<\\/repository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' FILE")
        1 * getPipelineMock('sh')("sed -i '/<pluginRepository>/,/<\\/pluginRepository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' FILE")
        settingsFile == 'FILE'
    }

    def "[MavenSettingsConfig.groovy] clone ok"() {
        setup:
        steps.env = ['MAVEN_SETTINGS_XML':'settingsFileId']
        def props = new Properties()
        props.put('key1', 'value1')
        def mvnCfg = new MavenSettingsConfig(steps)
            .withSettingsXmlFile('SETTINGS_FILE')
            .withDependencyRepositoryInSettings('ID', 'URL')
            .withMirrorDisabledForRepoInSettings('DISABLED_ID')
            .withSnapshotsDisabledInSettings()
        when:
        mvnCfg.createSettingsFile()
        def newCfg = mvnCfg
            .clone()
            .withSettingsXmlId('anyId')
        def newSettingsFile = newCfg.createSettingsFile()
        def settingsFile = mvnCfg.createSettingsFile()
        then:
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

    def "[MavenSettingsConfig.groovy] printSettings"() {
        setup:
        def mvnCfg = new MavenSettingsConfig(steps).withSettingsXmlFile('SETTINGS_FILE')
        when:
        def settingsFile = mvnCfg.printSettings().createSettingsFile()
        then:
        1 * getPipelineMock('sh')('cat SETTINGS_FILE')
    }

}
