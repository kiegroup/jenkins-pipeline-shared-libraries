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

    def "[maven.groovy] run mvn versions set"() {
        setup:
            def String newVersion = '3.6.2'
        when:
            mavenGroovy.mvnVersionsSet(newVersion) 
        then:
            1 * getPipelineMock("sh")("mvn -B -N -e versions:set -Dfull -DnewVersion=${newVersion} -DallowSnapshots=false -DgenerateBackupPoms=false")
    }

    def "[maven.groovy] run mvn versions set with allow snapshots"() {
        setup:
            def String newVersion = '3.6.2'
        when:
            mavenGroovy.mvnVersionsSet(newVersion, true) 
        then:
            1 * getPipelineMock("sh")("mvn -B -N -e versions:set -Dfull -DnewVersion=${newVersion} -DallowSnapshots=true -DgenerateBackupPoms=false")
    }

    def "[maven.groovy] run mvn versions update parent"() {
        setup:
            def String newVersion = '3.6.2'
        when:
            mavenGroovy.mvnVersionsUpdateParent(newVersion) 
        then:
            1 * getPipelineMock("sh")("mvn -B -N -e versions:update-parent -Dfull -DparentVersion=[${newVersion}] -DallowSnapshots=false -DgenerateBackupPoms=false")
    }

    def "[maven.groovy] run mvn versions update parent with allow snapshots"() {
        setup:
            def String newVersion = '3.6.2'
        when:
            mavenGroovy.mvnVersionsUpdateParent(newVersion, true) 
        then:
            1 * getPipelineMock("sh")("mvn -B -N -e versions:update-parent -Dfull -DparentVersion=[${newVersion}] -DallowSnapshots=true -DgenerateBackupPoms=false")
    }

    def "[maven.groovy] run mvn versions update child modules"() {
        when:
            mavenGroovy.mvnVersionsUpdateChildModules() 
        then:
            1 * getPipelineMock("sh")('mvn -B -N -e versions:update-child-modules -Dfull -DallowSnapshots=false -DgenerateBackupPoms=false')
    }

    def "[maven.groovy] run mvn versions update child modules with allow snapshots"() {
        when:
            mavenGroovy.mvnVersionsUpdateChildModules(true) 
        then:
            1 * getPipelineMock("sh")('mvn -B -N -e versions:update-child-modules -Dfull -DallowSnapshots=true -DgenerateBackupPoms=false')
    }

   def "[maven.groovy] run mvn versions update parent and child modules"() {
        setup:
            def String newVersion = '3.6.2'
        when:
            mavenGroovy.mvnVersionsUpdateParentAndChildModules(newVersion) 
        then:
            1 * getPipelineMock("sh")("mvn -B -N -e versions:update-parent -Dfull -DparentVersion=[${newVersion}] -DallowSnapshots=false -DgenerateBackupPoms=false")
            1 * getPipelineMock("sh")('mvn -B -N -e versions:update-child-modules -Dfull -DallowSnapshots=false -DgenerateBackupPoms=false')
    }

    def "[maven.groovy] run mvn versions update parent and child modules with allow snapshots"() {
        setup:
            def String newVersion = '3.6.2'
        when:
            mavenGroovy.mvnVersionsUpdateParentAndChildModules(newVersion, true) 
        then:
            1 * getPipelineMock("sh")("mvn -B -N -e versions:update-parent -Dfull -DparentVersion=[${newVersion}] -DallowSnapshots=true -DgenerateBackupPoms=false")
            1 * getPipelineMock("sh")('mvn -B -N -e versions:update-child-modules -Dfull -DallowSnapshots=true -DgenerateBackupPoms=false')
    }
}