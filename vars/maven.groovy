import java.util.Properties
import org.kie.jenkins.MavenCommand

def runMaven(String goals, List options = [], Properties properties = null, String logFileName = null) {
    new MavenCommand(this)
            .withOptions(options)
            .withProperties(properties)
            .withLogFileName(logFileName)
            .run(goals)
}

def runMaven(String goals, boolean skipTests, List options = [], String logFileName = null) {
    new MavenCommand(this)
            .withOptions(options)
            .skipTests(skipTests)
            .withLogFileName(logFileName)
            .run(goals)
}

def runMavenWithSettings(String settingsXmlId, String goals, Properties properties, String logFileName = null) {
    new MavenCommand(this, ['-fae'])
            .withSettingsXmlId(settingsXmlId)
            .withProperties(properties)
            .withLogFileName(logFileName)
            .run(goals)
}

def runMavenWithSettings(String settingsXmlId, String goals, boolean skipTests, String logFileName = null) {
    new MavenCommand(this, ['-fae'])
            .withSettingsXmlId(settingsXmlId)
            .skipTests(skipTests)
            .withLogFileName(logFileName)
            .run(goals)
}

def runMavenWithSubmarineSettings(String goals, boolean skipTests, String logFileName = null) {
    runMavenWithSettings(getSubmarineSettingsXmlId(), goals, skipTests, logFileName)
}

def runMavenWithSubmarineSettings(String goals, Properties properties, String logFileName = null) {
    runMavenWithSettings(getSubmarineSettingsXmlId(), goals, properties, logFileName)
}

String getSubmarineSettingsXmlId() {
    return '9239af2e-46e3-4ba3-8dd6-1a814fc8a56d'
}

/**
 *
 * @param settingsXmlId settings.xml file
 * @param goals maven gals
 * @param sonarCloudId Jenkins token for SonarCloud*
 */
def runMavenWithSettingsSonar(String settingsXmlId, String goals, String sonarCloudId, String logFileName = null) {
    withCredentials([string(credentialsId: sonarCloudId, variable: 'TOKEN')]) {
        new MavenCommand(this)
                .withSettingsXmlId(settingsXmlId)
                .withProperty('sonar.login', "${TOKEN}")
                .withLogFileName(logFileName)
                .run(goals)
    }
}

def mvnVersionsSet(String newVersion, boolean allowSnapshots = false) {
    mvnVersionsSet(new MavenCommand(this), newVersion, allowSnapshots)
}

def mvnVersionsSet(MavenCommand mvnCmd, String newVersion, boolean allowSnapshots = false) {
    mvnCmd.clone()
            .withOptions(['-N', '-e'])
            .withProperty('full')
            .withProperty('newVersion', newVersion)
            .withProperty('allowSnapshots', allowSnapshots)
            .withProperty('generateBackupPoms', false)
            .run('versions:set')
}

def mvnVersionsUpdateParent(String newVersion, boolean allowSnapshots = false) {
    mvnVersionsUpdateParent(new MavenCommand(this), newVersion, allowSnapshots)
}

def mvnVersionsUpdateParent(MavenCommand mvnCmd, String newVersion, boolean allowSnapshots = false) {
    mvnCmd.clone()
            .withOptions(['-N', '-e'])
            .withProperty('full')
            .withProperty('parentVersion', "[${newVersion}]")
            .withProperty('allowSnapshots', allowSnapshots)
            .withProperty('generateBackupPoms', false)
            .run('versions:update-parent')
}

def mvnVersionsUpdateChildModules(boolean allowSnapshots = false) {
    mvnVersionsUpdateChildModules(new MavenCommand(this), allowSnapshots)
}

def mvnVersionsUpdateChildModules(MavenCommand mvnCmd, boolean allowSnapshots = false) {
    mvnCmd.clone()
            .withOptions(['-N', '-e'])
            .withProperty('full')
            .withProperty('allowSnapshots', allowSnapshots)
            .withProperty('generateBackupPoms', false)
            .run('versions:update-child-modules')
}

def mvnVersionsUpdateParentAndChildModules(String newVersion, boolean allowSnapshots = false) {
    mvnVersionsUpdateParentAndChildModules(new MavenCommand(this), newVersion, allowSnapshots)
}

def mvnVersionsUpdateParentAndChildModules(MavenCommand mvnCmd, String newVersion, boolean allowSnapshots = false) {
    mvnVersionsUpdateParent(mvnCmd, newVersion, allowSnapshots)
    mvnVersionsUpdateChildModules(mvnCmd, allowSnapshots)
}

def mvnSetVersionProperty(String property, String newVersion) {
    mvnSetVersionProperty(new MavenCommand(this), property, newVersion)
}

def mvnSetVersionProperty(MavenCommand mvnCmd, String property, String newVersion) {
    mvnCmd.clone()
            .withOptions(['-e'])
            .withProperty('property', property)
            .withProperty('newVersion', newVersion)
            .withProperty('allowSnapshots', true)
            .withProperty('generateBackupPoms', false)
            .run('versions:set-property')
}

def uploadLocalArtifacts(String mvnUploadCredsId, String artifactDir, String repoUrl) {
    def zipFileName = 'artifacts'
    withCredentials([usernameColonPassword(credentialsId: mvnUploadCredsId, variable: 'kieUnpack')]) {
        dir(artifactDir) {
            sh "zip -r ${zipFileName} ."
            sh "curl --silent --upload-file ${zipFileName}.zip -u ${kieUnpack} -v ${repoUrl}"
        }
    }
}

def getLatestArtifactVersionFromRepository(String repositoryUrl, String groupId, String artifactId) {
    def groupIdArtifactId = "${groupId.replaceAll("\\.", "/")}/${artifactId}"
    def metadata = new XmlSlurper().parse("${repositoryUrl}/${groupIdArtifactId}/maven-metadata.xml")
    return metadata.versioning?.latest?.text()
}

def getLatestArtifactVersionPrefixFromRepository(String repositoryUrl, String groupId, String artifactId, String versionPrefix) {
    def groupIdArtifactId = "${groupId.replaceAll("\\.", "/")}/${artifactId}"
    def metadata = new XmlSlurper().parse("${repositoryUrl}/${groupIdArtifactId}/maven-metadata.xml")
    return metadata.versioning?.versions?.childNodes().collect{ it.text()}.findAll{it.startsWith(versionPrefix)}.max()
}

/*
* Clean Maven repository on the node
*/

void cleanRepository() {
    sh 'rm -rf $HOME/.m2/repository'
}