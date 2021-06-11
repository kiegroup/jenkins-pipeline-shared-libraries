package org.kie.jenkins

def class MavenCommand {

    def steps

    String directory = ''

    String settingsXmlConfigFileId = ''
    String settingsXmlPath = ''
    Map dependenciesRepositoriesInSettings = [:]
    List disabledMirrorRepoInSettings = []
    boolean disableSnapshotsInSettings = false

    List mavenOptions = []
    Map properties = [:]
    String logFileName = ''
    List profiles = []

    boolean printSettings = false
    boolean returnStdout = false

    MavenCommand(steps){
        this.steps = steps
    }

    MavenCommand(steps, List defaultOpts){
        this(steps)
        this.mavenOptions.addAll(defaultOpts)
    }

    def run(String goals) {
        String cmd = getFullRunCommand(goals)
        if(directory) {
            steps.dir(directory) {
                return runCommand(cmd)
            }
        } else {
            return runCommand(cmd)
        }
    }

    String getFullRunCommand(String goals) {
        def cmdBuilder = new StringBuilder("mvn -B")

        // Retrieve settings file from id if given
        String settingsFile = this.settingsXmlPath
        if(this.settingsXmlConfigFileId){
            steps.configFileProvider([steps.configFile(fileId: this.settingsXmlConfigFileId, targetLocation: 'maven-settings.xml', variable: 'MAVEN_SETTINGS_XML')]) {
                settingsFile = steps.env['MAVEN_SETTINGS_XML']
            }
        }
        if(settingsFile) {
            this.dependenciesRepositoriesInSettings.each { setRepositoryInSettings(settingsFile, it.key, it.value) }
            cmdBuilder.append(" -s ${settingsFile}")

            this.disabledMirrorRepoInSettings.each {
                disableMirrorForRepoInSettings(settingsFile, it)
            }

            if(this.disableSnapshotsInSettings) {
                steps.sh "sed -i '/<repository>/,/<\\/repository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' ${settingsFile}"
                steps.sh "sed -i '/<pluginRepository>/,/<\\/pluginRepository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' ${settingsFile}"
            }

            if(this.printSettings){
                steps.sh "cat ${settingsFile}"
            }
        }

        if(this.mavenOptions.size() > 0){
            cmdBuilder.append(' ').append(this.mavenOptions.join(' '))
        }
        cmdBuilder.append(' ').append(goals)
        if(this.profiles.size() > 0){
            cmdBuilder.append(' -P').append(this.profiles.join(','))
        }
        if(this.properties.size()){
            cmdBuilder.append(' ').append(this.properties.collect{ it.value != '' ? "-D${it.key}=${it.value}" : "-D${it.key}" }.join(' '))
        }
        if(this.logFileName){
            cmdBuilder.append(" | tee \$WORKSPACE/${this.logFileName} ; test \${PIPESTATUS[0]} -eq 0")
        }
        return cmdBuilder.toString()
    }

    private def runCommand(String cmd){
        return steps.sh(script: cmd, returnStdout: this.returnStdout)
    }

    MavenCommand inDirectory(String directory) {
        this.directory = directory
        return this
    }

    /**
    * IF set, override `withSettingsXmlFile`
    **/
    MavenCommand withSettingsXmlId(String settingsXmlId){
        this.settingsXmlConfigFileId = settingsXmlId
        return this
    }

    MavenCommand withSettingsXmlFile(String settingsXmlPath){
        assert settingsXmlPath: 'Trying to set an empty settings xml path'
        this.settingsXmlPath = settingsXmlPath
        return this
    }

    MavenCommand withDependencyRepositoryInSettings(String repoId, String repoUrl){
        this.dependenciesRepositoriesInSettings.put(repoId, repoUrl)
        return this
    }

    MavenCommand withDependencyRepositoriesInSettings(Map repositories = [:]){
        this.dependenciesRepositoriesInSettings.putAll(repositories)
        return this
    }

    MavenCommand withMirrorDisabledForRepoInSettings(String repoId) {
        if(!this.disabledMirrorRepoInSettings.find { it == repoId } ) {
            this.disabledMirrorRepoInSettings.add(repoId)
        }
        return this
    }

    MavenCommand withSnapshotsDisabledInSettings() {
        this.disableSnapshotsInSettings = true
        return this
    }

    MavenCommand withOptions(List opts) {
        this.mavenOptions.addAll(opts)
        return this
    }

    MavenCommand skipTests(boolean skipTests=true) {
        return withProperty('skipTests', skipTests)
    }

    MavenCommand withProfiles(List profiles) {
        this.profiles.addAll(profiles)
        return this
    }

    MavenCommand withProperty(String key, Object value = '') {
        this.properties.put(key, value)
        return this
    }

    MavenCommand withProperties(Properties properties) {
        withPropertyMap(properties ?: [:])
        return this
    }

    MavenCommand withPropertyMap(Map properties) {
        this.properties.putAll(properties)
        return this
    }

    MavenCommand withLogFileName(String logFileName){
        this.logFileName = logFileName
        return this
    }

    MavenCommand withDeployRepository(String deployRepository){
        assert deployRepository: 'Trying to add an empty deploy repository'
        withProperty('altDeploymentRepository', "runtimes-artifacts::default::${deployRepository}")
        withProperty('enforcer.skip', true)
    }

    MavenCommand withLocalDeployFolder(String localDeployFolder){
        assert localDeployFolder: 'Trying to add an empty local deploy folder'
        withProperty('altDeploymentRepository', "local::default::file://${localDeployFolder}")
    }

    MavenCommand clone(){
        def newCmd = new MavenCommand(this.steps)
            .withOptions(this.mavenOptions)
            .withPropertyMap(this.properties)
            .withProfiles(this.profiles)
            .withDependencyRepositoriesInSettings(this.dependenciesRepositoriesInSettings)
        if(this.settingsXmlConfigFileId){
            newCmd.withSettingsXmlId(this.settingsXmlConfigFileId)
        }
        this.disabledMirrorRepoInSettings.each {
            newCmd.withMirrorDisabledForRepoInSettings(it)
        }
        if(this.disableSnapshotsInSettings) {
            newCmd.withSnapshotsDisabledInSettings()
        }
        if(this.settingsXmlPath){
            newCmd.withSettingsXmlFile(this.settingsXmlPath)
        }
        if(this.logFileName){
            newCmd.withLogFileName(this.logFileName)
        }
        if(this.directory){
            newCmd.inDirectory(this.directory)
        }
        if(this.returnStdout){
            newCmd.returnOutput()
        }
        return newCmd
    }

    MavenCommand returnOutput(){
        this.returnStdout = true
        return this
    }

    MavenCommand printSettings(){
        this.printSettings = true
        return this
    }

    private void setRepositoryInSettings(String settingsFilePath, String repoId, String repoUrl) {
        def depsRepositoryContent = "<id>${repoId}</id><name>${repoId}</name><url>${repoUrl}</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases>"
        steps.sh """
            sed -i 's|<repositories>|<repositories><!-- BEGIN added repository --><repository>${depsRepositoryContent}</repository><!-- END added repository -->|g' ${settingsFilePath}
            sed -i 's|<pluginRepositories>|<pluginRepositories><!-- BEGIN added repository --><pluginRepository>${depsRepositoryContent}</pluginRepository><!-- END added repository -->|g' ${settingsFilePath}
        """
        disableMirrorForRepoInSettings(settingsFilePath, repoId)

        withProperty('enforcer.skip', true)
    }

    private void disableMirrorForRepoInSettings(String settingsFilePath, String repoId) {
        steps.sh "sed -i 's|</mirrorOf>|,!${repoId}</mirrorOf>|g' ${settingsFilePath}"
    }
}
