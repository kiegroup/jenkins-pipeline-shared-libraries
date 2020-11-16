import java.util.Properties

def runMaven(String goals, List options=[], Properties properties=null, String logFileName = null) {
    new MavenCommand(this)
        .withOptions(options)
        .withProperties(properties)
        .withLogFileName(logFileName)
        .run(goals)
}

def runMaven(String goals, boolean skipTests, List options=[], String logFileName = null) {
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

String getSubmarineSettingsXmlId(){
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
    new MavenCommand(this, ['-N', '-e'])
        .withProperty('full')
        .withProperty('newVersion', newVersion)
        .withProperty('allowSnapshots', allowSnapshots)
        .withProperty('generateBackupPoms', false)
        .run('versions:set')
}

def mvnVersionsUpdateParent(String newVersion, boolean allowSnapshots = false) {
    new MavenCommand(this, ['-N', '-e'])
        .withProperty('full')
        .withProperty('parentVersion', "[${newVersion}]")
        .withProperty('allowSnapshots', allowSnapshots)
        .withProperty('generateBackupPoms', false)
        .run('versions:update-parent')
}

def mvnVersionsUpdateChildModules(boolean allowSnapshots = false) {
    new MavenCommand(this, ['-N', '-e'])
        .withProperty('full')
        .withProperty('allowSnapshots', allowSnapshots)
        .withProperty('generateBackupPoms', false)
        .run('versions:update-child-modules')
}

def mvnVersionsUpdateParentAndChildModules(String newVersion, boolean allowSnapshots = false) {
    mvnVersionsUpdateParent(newVersion, allowSnapshots)
    mvnVersionsUpdateChildModules(allowSnapshots)
}

def mvnSetVersionProperty(String property, String newVersion) {
    new MavenCommand(this, ['-e'])
        .withProperty('property', property)
        .withProperty('newVersion', newVersion)
        .withProperty('allowSnapshots', true)
        .withProperty('generateBackupPoms', false)
        .run('versions:set-property')
}