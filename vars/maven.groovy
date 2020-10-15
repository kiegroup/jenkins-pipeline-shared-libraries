import java.util.Properties

def runMaven(String goals, List options=[], Properties properties=null, String logFileName = null) {
    def mvnCommand = "mvn -B"
    if(options.size() > 0){
        mvnCommand += " ${options.join(' ')}"
    }
    mvnCommand += " ${goals}"
    if(properties){
        mvnCommand += " ${properties.collect{ key, value -> "-D$key=$value" }.join(' ')}"
    }
    if(logFileName){
        mvnCommand += " | tee \$WORKSPACE/${logFileName} ; test \${PIPESTATUS[0]} -eq 0"
    }
    sh mvnCommand
}

def runMaven(String goals, boolean skipTests, List options=[], String logFileName = null) {
    Properties properties = new Properties()
    properties.put('skipTests', skipTests)
    runMaven(goals, options, properties, logFileName)
}

def runMavenWithSettings(String settingsXmlId, String goals, Properties properties, String logFileName = null) {
    configFileProvider([configFile(fileId: settingsXmlId, variable: 'MAVEN_SETTINGS_XML')]) {
        runMaven(goals, ["-s ${MAVEN_SETTINGS_XML}", '-fae'], properties, logFileName)
    }
}

def runMavenWithSettings(String settingsXmlId, String goals, boolean skipTests, String logFileName = null) {
    Properties properties = new Properties()
    properties.put('skipTests', skipTests)
    runMavenWithSettings(settingsXmlId, goals, properties, logFileName)
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
    configFileProvider([configFile(fileId: settingsXmlId, variable: 'MAVEN_SETTINGS_XML')]) {
        withCredentials([string(credentialsId: sonarCloudId, variable: 'TOKEN')]) {
            Properties props = new Properties()
            props.setProperty('sonar.login', "${TOKEN}")
            runMaven(goals, ["-s ${MAVEN_SETTINGS_XML}"], props, logFileName)
        }
    }
}

def mvnVersionsSet(String newVersion, boolean allowSnapshots = false) {
    sh "mvn -B -N -e versions:set -Dfull -DnewVersion=${newVersion} -DallowSnapshots=${allowSnapshots} -DgenerateBackupPoms=false"
}

def mvnVersionsUpdateParent(String newVersion, boolean allowSnapshots = false) {
    sh "mvn -B -N -e versions:update-parent -Dfull -DparentVersion=[${newVersion}] -DallowSnapshots=${allowSnapshots} -DgenerateBackupPoms=false"
}

def mvnVersionsUpdateChildModules(boolean allowSnapshots = false) {
    sh "mvn -B -N -e versions:update-child-modules -Dfull -DallowSnapshots=${allowSnapshots} -DgenerateBackupPoms=false"
}

def mvnVersionsUpdateParentAndChildModules(String newVersion, boolean allowSnapshots = false) {
    mvnVersionsUpdateParent(newVersion, allowSnapshots)
    mvnVersionsUpdateChildModules(allowSnapshots)
}