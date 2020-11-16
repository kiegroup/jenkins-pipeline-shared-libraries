def class MavenCommand {

    def steps

    boolean returnStdout = false

    String settingsXmlPath = ''
    List mavenOptions = []
    Map properties = [:]
    String logFileName = null
    List profiles = []

    MavenCommand(steps){
        this.steps = steps
    }

    MavenCommand(steps, List defaultOpts){
        this(steps)
        this.mavenOptions.addAll(defaultOpts)
    }

    def run(String goals) {
        String cmd = 'mvn -B'
        if(this.settingsXmlPath) {
            cmd += " -s ${this.settingsXmlPath}"
        }
        if(this.mavenOptions.size() > 0){
            cmd += ' ' + this.mavenOptions.join(' ')
        }
        cmd += ' ' + goals
        if(this.profiles.size() > 0){
            cmd += ' -P' + this.profiles.join(',')
        }
        if(this.properties.size()){
            cmd += ' ' + this.properties.collect{ it.value != '' ? "-D${it.key}=${it.value}" : "-D${it.key}" }.join(' ')
        }
        if(this.logFileName){
            cmd += " | tee \$WORKSPACE/${this.logFileName} ; test \${PIPESTATUS[0]} -eq 0"
        }

        return steps.sh(script: cmd, returnStdout: this.returnStdout)
    }

    MavenCommand withSettingsXmlId(String settingsXmlId){
        steps.configFileProvider([steps.configFile(fileId: settingsXmlId, variable: 'MAVEN_SETTINGS_XML')]) {
            withSettingsXmlFile(steps.env['MAVEN_SETTINGS_XML'])
        }
        return this
    }

    MavenCommand withSettingsXmlFile(String settingsXmlPath){
        assert settingsXmlPath: 'Trying to set an empty settings xml path'
        this.settingsXmlPath = settingsXmlPath
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
        if(this.settingsXmlPath){
            newCmd.withSettingsXmlFile(this.settingsXmlPath)
        }
        if(this.logFileName){
            newCmd.withLogFileName(this.logFileName)
        }
        return newCmd
    }

    MavenCommand returnOutput(){
        this.returnStdout = true
        return this
    }
}