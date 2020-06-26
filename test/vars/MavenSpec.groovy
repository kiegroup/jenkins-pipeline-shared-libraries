import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader

class MavenSpec extends JenkinsPipelineSpecification {
    def mavenGroovy = null

    def setup() {
        explicitlyMockPipelineVariable("out")
        mavenGroovy = loadPipelineScriptForTest("vars/maven.groovy")
    }

    def "[maven.groovy] run Maven with settings with log file"() {
        setup:
        mavenGroovy.metaClass.MAVEN_SETTINGS_XML = 'settingsFileId'
        Properties properties = new Properties()
        properties.put('property1', 'value1')
        when:
        mavenGroovy.runMavenWithSettings("settings.xml", "clean install", properties, "logFile.txt")
        then:
        1 * getPipelineMock("sh")('mvn -B -s settingsFileId -fae clean install -Dproperty1=value1 | tee $WORKSPACE/logFile.txt ; test ${PIPESTATUS[0]} -eq 0')
    }

    def "[maven.groovy] run Maven with settings without log file"() {
        setup:
        mavenGroovy.metaClass.MAVEN_SETTINGS_XML = 'settingsFileId'
        Properties properties = new Properties()
        properties.put('property1b', 'value1b')
        when:
        mavenGroovy.runMavenWithSettings("settings.xml", "clean install", properties)
        then:
        1 * getPipelineMock("sh")('mvn -B -s settingsFileId -fae clean install -Dproperty1b=value1b')
    }

    def "[maven.groovy] run Maven with settings without properties"() {
        setup:
        mavenGroovy.metaClass.MAVEN_SETTINGS_XML = 'settingsFileId'
        Properties properties = new Properties()
        when:
        mavenGroovy.runMavenWithSettings("settings.xml", "clean install", properties, "logFile.txt")
        then:
        1 * getPipelineMock("sh")('mvn -B -s settingsFileId -fae clean install | tee $WORKSPACE/logFile.txt ; test ${PIPESTATUS[0]} -eq 0')
    }

    def "[maven.groovy] run Maven sonar settings with log file"() {
        setup:
        mavenGroovy.metaClass.MAVEN_SETTINGS_XML = 'settingsFileId'
        mavenGroovy.metaClass.TOKEN = 'tokenId'
        when:
        mavenGroovy.runMavenWithSettingsSonar("settings.xml", "clean install", "sonarCloudId", "logFile.txt")
        then:
        1 * getPipelineMock("sh")('mvn -B -s settingsFileId -Dsonar.login=tokenId clean install | tee $WORKSPACE/logFile.txt ; test ${PIPESTATUS[0]} -eq 0')
    }

    def "[maven.groovy] run Maven sonar settings without log file"() {
        setup:
        mavenGroovy.metaClass.MAVEN_SETTINGS_XML = 'settingsFileId'
        mavenGroovy.metaClass.TOKEN = 'tokenId'
        when:
        mavenGroovy.runMavenWithSettingsSonar("settings.xml", "clean install", "sonarCloudId")
        then:
        1 * getPipelineMock("sh")('mvn -B -s settingsFileId -Dsonar.login=tokenId clean install')
    }

    def "[maven.groovy] run with Settings with log file"() {
        setup:
        mavenGroovy.metaClass.MAVEN_SETTINGS_XML = 'settingsFileId'
        mavenGroovy.metaClass.runMavenWithSettings(String, String, Properties, String) >> {}
        Properties properties = new Properties()
        properties.put('skipTests', true)
        when:
        mavenGroovy.runMavenWithSettings("settings.xml", "clean install", true, "logFile.txt")
        then:
        1 * getPipelineMock("sh")('mvn -B -s settingsFileId -fae clean install -DskipTests=true | tee $WORKSPACE/logFile.txt ; test ${PIPESTATUS[0]} -eq 0')
    }

    def "[maven.groovy] run with Settings without log file"() {
        setup:
        mavenGroovy.metaClass.MAVEN_SETTINGS_XML = 'settingsFileId'
        mavenGroovy.metaClass.runMavenWithSettings(String, String, Properties, String) >> {}
        when:
        mavenGroovy.runMavenWithSettings("settings.xml", "clean install", false)
        then:
        1 * getPipelineMock("sh")('mvn -B -s settingsFileId -fae clean install -DskipTests=false')
    }

    def "[maven.groovy] run with Submarine Settings without properties and without log file"() {
        setup:
        mavenGroovy.metaClass.MAVEN_SETTINGS_XML = 'settingsFileId'
        mavenGroovy.metaClass.runMavenWithSettings(String, String, Properties, String) >> {}
        when:
        mavenGroovy.runMavenWithSubmarineSettings("clean install", false)
        then:
        1 * getPipelineMock("sh")('mvn -B -s settingsFileId -fae clean install -DskipTests=false')
    }

    def "[maven.groovy] run with Submarine Settings with properties and without log file"() {
        setup:
        mavenGroovy.metaClass.MAVEN_SETTINGS_XML = 'settingsFileId'
        mavenGroovy.metaClass.runMavenWithSettings(String, String, Properties, String) >> {}
        Properties properties = new Properties()
        when:
        mavenGroovy.runMavenWithSubmarineSettings("clean install", properties)
        then:
        1 * getPipelineMock("sh")('mvn -B -s settingsFileId -fae clean install')
    }

    def "[maven.groovy] artifactExists"() {
        setup:
        def settingsXmlId = 'settingsFileId'
        def artifact = 'org.kie:kie-parent:7.40.0-SNAPSHOT:pom'
        mavenGroovy.metaClass.MAVEN_SETTINGS_XML = settingsXmlId
        when:
        def result = mavenGroovy.artifactExists(settingsXmlId, artifact)
        then:
        1 * getPipelineMock("sh")("mvn dependency:get -Dartifact=${artifact} -DremoteRepositories=central::default::https://repo.maven.apache.org/maven2 -s ${settingsXmlId}")
        1 * getPipelineMock("configFile.call")(['fileId': 'settingsFileId', 'variable': 'MAVEN_SETTINGS_XML']) >> { return 'configFile' }
        1 * getPipelineMock("configFileProvider.call")(['configFile'], _ as Closure) >> true
        result == true
    }

    def "[maven.groovy] artifactExists false"() {
        setup:
        def settingsXmlId = 'settingsFileId'
        def artifact = 'org.kie:kie-parent:7.40.0-SNAPSHOT:pom'
        mavenGroovy.metaClass.MAVEN_SETTINGS_XML = settingsXmlId
        when:
        def result = mavenGroovy.artifactExists(settingsXmlId, artifact)
        then:
        1 * getPipelineMock("sh")("mvn dependency:get -Dartifact=${artifact} -DremoteRepositories=central::default::https://repo.maven.apache.org/maven2 -s ${settingsXmlId}") >> { throw new Exception('mock error') }
        1 * getPipelineMock("configFile.call")(['fileId': 'settingsFileId', 'variable': 'MAVEN_SETTINGS_XML']) >> { return 'configFile' }
        1 * getPipelineMock("configFileProvider.call")(['configFile'], _ as Closure) >> false
        result == false
    }

    def "[maven.groovy] getPomArtifact"() {
        setup:
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(MavenSpec.class.getResourceAsStream('/pom.xml'));
        when:
        def result = mavenGroovy.getPomArtifact('/pom.xml')
        then:
        1 * getPipelineMock("readMavenPom")(['file': '/pom.xml']) >> { return model }
        'org.kie:jenkins-pipeline-shared-libraries-test-resources:1.0.0:jar' == result
    }

    def "[maven.groovy] getPomArtifact pom2.xml"() {
        setup:
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(MavenSpec.class.getResourceAsStream('/pom2.xml'));
        when:
        def result = mavenGroovy.getPomArtifact('/pom2.xml')
        then:
        1 * getPipelineMock("readMavenPom")(['file': '/pom2.xml']) >> { return model }
        'org.kie:optaweb-employee-rostering:7.40.0-SNAPSHOT:pom' == result
    }
}