def runMavenWithSettings(String settingsXmlId, String goals, boolean skipTests) {
    configFileProvider([configFile(fileId: settingsXmlId, variable: 'MAVEN_SETTINGS_XML')]) {
        def mvnCommand = 'mvn -s $MAVEN_SETTINGS_XML -fae ' + goals
        if (skipTests) {
            mvnCommand = mvnCommand + ' -DskipTests'
        }
        sh mvnCommand
    }
}

def runMavenWithSubmarineSettings(String goals, boolean skipTests) {
    runMavenWithSettings('9239af2e-46e3-4ba3-8dd6-1a814fc8a56d', goals, skipTests)
}
