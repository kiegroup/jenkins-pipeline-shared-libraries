import java.util.Properties

def runMaven(String goals, List options=[], Properties properties=null, String logFileName = null) {
    def mvnCommand = "mvn -B ${options.size() > 0 ? options.join(' ') + ' ' : ''}${goals}"
    if(properties){
        properties.each { key, value ->
            mvnCommand += " -D$key=$value"
        }
    }
    if(logFileName){
        mvnCommand += ' | tee $WORKSPACE/'+ logFileName + ' ; test ${PIPESTATUS[0]} -eq 0'
    }
    sh mvnCommand
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
    runMavenWithSettings('9239af2e-46e3-4ba3-8dd6-1a814fc8a56d', goals, skipTests, logFileName)
}

def runMavenWithSubmarineSettings(String goals, Properties properties, String logFileName = null) {
    runMavenWithSettings('9239af2e-46e3-4ba3-8dd6-1a814fc8a56d', goals, properties, logFileName)
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