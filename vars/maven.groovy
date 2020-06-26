import java.util.Properties

def runMavenWithSettings(String settingsXmlId, String goals, Properties properties, String logFileName = null) {
    configFileProvider([configFile(fileId: settingsXmlId, variable: 'MAVEN_SETTINGS_XML')]) {
        def propertiesString = ''

        properties.each { key, value ->
            propertiesString += " -D$key=$value"
        }

        def teeCommand = logFileName ? ' | tee $WORKSPACE/' + logFileName + ' ; test ${PIPESTATUS[0]} -eq 0' : ''
        def mvnCommand = "mvn -B -s $MAVEN_SETTINGS_XML -fae ${goals}${propertiesString}${teeCommand}"
        sh mvnCommand
    }
}

def runMavenWithSettings(String settingsXmlId, String goals, boolean skipTests, String logFileName = null) {
    Properties properties = new Properties()
    properties.put('skipTests', skipTests)
    runMavenWithSettings(settingsXmlId, goals, properties, logFileName)
}

def runMavenWithSubmarineSettings(String goals, boolean skipTests, String logFileName = null) {
    runMavenWithSettings('9239af2e-46e3-4ba3-8dd6-1a814fc8a56d', goals, skipTests, logFileName)
}

def runMavenWithSubmarineSettings(String goals, Properties properties, String logFileName = null) {
    runMavenWithSettings('9239af2e-46e3-4ba3-8dd6-1a814fc8a56d', goals, properties, logFileName)
}

/**
 *
 * @param settingsXmlId settings.xml file ID
 * @param goals maven gals
 * @param sonarCloudId Jenkins token for SonarCloud*
 */
def runMavenWithSettingsSonar(String settingsXmlId, String goals, String sonarCloudId, String logFileName = null) {
    configFileProvider([configFile(fileId: settingsXmlId, variable: 'MAVEN_SETTINGS_XML')]) {
        withCredentials([string(credentialsId: sonarCloudId, variable: 'TOKEN')]) {
            def teeCommand = logFileName ? ' | tee $WORKSPACE/' + logFileName + ' ; test ${PIPESTATUS[0]} -eq 0' : ''
            sh "mvn -B -s $MAVEN_SETTINGS_XML -Dsonar.login=${TOKEN} ${goals}${teeCommand}"
        }
    }
}

/**
 *
 * @param settingsXmlId settings.xml file ID
 * @param artifact A string of the form groupId:artifactId:version[:packaging[:classifier]].
 * @param remoteRepository Repositories in the format id::[layout]::url or just url, separated by comma. ie. central::default::https://repo.maven.apache.org/maven2,myrepo::::https://repo.acme.com,https://repo.acme2.com
 * @return if the artifact exists or not
 */
def artifactExists(String settingsXmlId, String artifact, String remoteRepository = 'central::default::https://repo.maven.apache.org/maven2') {
    configFileProvider([configFile(fileId: settingsXmlId, variable: 'MAVEN_SETTINGS_XML')]) {
        try {
            sh "mvn dependency:get -Dartifact=${artifact} -DremoteRepositories=${remoteRepository} -s $MAVEN_SETTINGS_XML"
            return true
        } catch (Exception e) {
            println "[WARNING] Error executing maven dependency:get ${e.getMessage()}"
            return false
        }
    }
}

/**
 * Reads a pom file and returns back the artifact String groupId:artifactId:version[:packaging[:classifier]] like org.kie:kie-parent:7.40.0-SNAPSHOT:pom
 * @param pomFilePath the path for the pom file
 * @return the pom artifact string
 */
def getPomArtifact(String pomFilePath) {
    def pom = readMavenPom file: pomFilePath
    def groupId = pom.groupId ?: pom.parent.groupId
    def artifactId = pom.artifactId ?: pom.parent.artifactId
    def version = pom.version ?: pom.parent.version
    assert groupId
    assert artifactId
    assert version
    return "${groupId}:${artifactId}:${version}:${pom.packaging}"
}