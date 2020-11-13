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
        def cmd = "mvn -B"
        if(this.settingsXmlPath) {
            cmd += " -s ${this.settingsXmlPath}"
        }
        if(this.mavenOptions.size() > 0){
            cmd += " ${this.mavenOptions.join(' ')}"
        }
        cmd += " ${goals}"
        if(this.profiles.size() > 0){
            cmd += " -P${this.profiles.join(',')}"
        }
        if(this.properties.size()){
            cmd += " ${this.properties.collect{ it.value != '' ? "-D${it.key}=${it.value}" : "-D${it.key}" }.join(' ')}"
        }
        if(this.logFileName){
            cmd += " | tee \$WORKSPACE/${logFileName} ; test \${PIPESTATUS[0]} -eq 0"
        }

        if(this.returnStdout){
            return steps.sh(script: cmd, returnStdout: this.returnStdout)
        }
        steps.sh cmd
    }

    def runClean() {
        return run('clean')
    }

    def runPackage(boolean clean = false) {
        return run(clean ? 'clean package' : 'package')
    }
    
    def runInstall(boolean clean = false) {
        return run(clean ? 'clean install' : 'install')
    }

    def runVerify(boolean clean = false) {
        return run(clean ? 'clean verify' : 'verify')
    }

    def runDeploy(boolean clean = false, String deployRepository = ''){
        if(deployRepository){
            withProperty('altDeploymentRepository', "runtimes-artifacts::default::${deployRepository}")
            withProperty('enforcer.skip', true)
        }
        return run(clean ? 'clean deploy' : 'deploy')
    }

    def runDeployLocally(String localDeployFolder, boolean clean = false){
        if(localDeployFolder){
            withProperty('altDeploymentRepository', "local::default::file://${localDeployFolder}")
        }
        return run(clean ? 'clean deploy' : 'deploy')
    }


    MavenCommand withSettingsXmlId(String settingsXmlId){
        steps.configFileProvider([steps.configFile(fileId: settingsXmlId, variable: 'MAVEN_SETTINGS_XML')]) {
            withSettingsXmlFile(steps.env['MAVEN_SETTINGS_XML'])
        }
        return this
    }

    MavenCommand withSettingsXmlFile(String settingsXmlPath){
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
        if(properties){
            properties.each { this.properties.put(it.key, it.value) }
        }
        return this
    }

    MavenCommand withPropertyMap(Map properties) {
        if(properties){
            this.properties.putAll(properties)
        }
        return this
    }

    MavenCommand withLogFileName(String logFileName){
        this.logFileName = logFileName
        return this
    }

    MavenCommand clone(){
        return new MavenCommand(this.steps)
            .withSettingsXmlFile(this.settingsXmlPath)
            .withOptions(this.mavenOptions)
            .withPropertyMap(this.properties)
            .withLogFileName(this.logFileName)
            .withProfiles(this.profiles)
    }

    MavenCommand returnOutput(){
        this.returnStdout = true
        return this
    }
}