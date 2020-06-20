import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class MavenSpec extends JenkinsPipelineSpecification {
    def mavenGroovy = null

    def setup() {
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

    def "[maven.groovy] update maven version in pom.xml "() {
        setup:
        def newVersion = "0.0.0"
        when:
        mavenGroovy.updateMavenVersion(newVersion)
        then:
        1 * getPipelineMock('maven.updateMavenVersion')('newVersion')
    }
}