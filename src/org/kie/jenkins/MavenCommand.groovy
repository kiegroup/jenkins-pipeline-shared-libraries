package org.kie.jenkins

class MavenCommand {

    def steps

    String directory = ''

    MavenSettingsConfig settingsConfig

    List mavenOptions = []
    Map properties = [:]
    String logFileName = ''
    List profiles = []

    boolean returnStdout = false

    MavenCommand(steps) {
        this.steps = steps
        settingsConfig = new MavenSettingsConfig(steps)
    }

    MavenCommand(steps, List defaultOpts) {
        this(steps)
        this.mavenOptions.addAll(defaultOpts)
    }

    def run(String goals) {
        String cmd = getFullRunCommand(goals)
        if (directory) {
            steps.dir(directory) {
                return runCommand(cmd)
            }
        } else {
            return runCommand(cmd)
        }
    }

    String getFullRunCommand(String goals) {
        def cmdBuilder = new StringBuilder('mvn -B')

        // Retrieve settings file from id if given
        String settingsFile = this.settingsConfig.createSettingsFile()
        cmdBuilder.append(settingsFile ? " -s ${settingsFile}" : '')

        if (this.mavenOptions.size() > 0) {
            cmdBuilder.append(' ').append(this.mavenOptions.join(' '))
        }
        cmdBuilder.append(' ').append(goals)
        if (this.profiles.size() > 0) {
            cmdBuilder.append(' -P').append(this.profiles.join(','))
        }
        if (this.properties.size()) {
            cmdBuilder.append(' ').append(this.properties.collect { it.value != '' ? "-D${it.key}=${it.value}" : "-D${it.key}" }.join(' '))
        }
        if (this.logFileName) {
            cmdBuilder.append(" | tee \$WORKSPACE/${this.logFileName} ; test \${PIPESTATUS[0]} -eq 0")
        }
        return cmdBuilder.toString()
    }

    private def runCommand(String cmd) {
        return steps.sh(script: cmd, returnStdout: this.returnStdout)
    }

    MavenCommand inDirectory(String directory) {
        this.directory = directory
        return this
    }

    /**
    * Overwrites all current settings config done.
    */
    MavenCommand withSettingsConfig(MavenSettingsConfig settingsConfig) {
        this.settingsConfig = settingsConfig
        return this
    }

    /**
    * IF set, override `withSettingsXmlFile`
    **/
    MavenCommand withSettingsXmlId(String settingsXmlId) {
        settingsConfig.withSettingsXmlId(settingsXmlId)
        return this
    }

    MavenCommand withSettingsXmlFile(String settingsXmlPath) {
        settingsConfig.withSettingsXmlFile(settingsXmlPath)
        return this
    }

    MavenCommand withDependencyRepositoryInSettings(String repoId, String repoUrl) {
        settingsConfig.withDependencyRepositoryInSettings(repoId, repoUrl)
        withProperty('enforcer.skip', true)
        return this
    }

    MavenCommand withDependencyRepositoriesInSettings(Map repositories = [:]) {
        settingsConfig.withDependencyRepositoriesInSettings(repositories)
        withProperty('enforcer.skip', true)
        return this
    }

    MavenCommand withMirrorDisabledForRepoInSettings(String repoId) {
        settingsConfig.withMirrorDisabledForRepoInSettings(repoId)
        return this
    }

    MavenCommand withSnapshotsDisabledInSettings() {
        settingsConfig.withSnapshotsDisabledInSettings()
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

    MavenCommand withLogFileName(String logFileName) {
        this.logFileName = logFileName
        return this
    }

    MavenCommand withDeployRepository(String deployRepository) {
        assert deployRepository: 'Trying to add an empty deploy repository'
        withProperty('altDeploymentRepository', "runtimes-artifacts::default::${deployRepository}")
        withProperty('enforcer.skip', true)
    }

    MavenCommand withLocalDeployFolder(String localDeployFolder) {
        assert localDeployFolder: 'Trying to add an empty local deploy folder'
        withProperty('altDeploymentRepository', "local::default::file://${localDeployFolder}")
    }

    MavenCommand clone() {
        def newCmd = new MavenCommand(this.steps)
            .withOptions(this.mavenOptions)
            .withPropertyMap(this.properties)
            .withProfiles(this.profiles)
            .withSettingsConfig(this.settingsConfig.clone())
        if (this.logFileName) {
            newCmd.withLogFileName(this.logFileName)
        }
        if (this.directory) {
            newCmd.inDirectory(this.directory)
        }
        if (this.returnStdout) {
            newCmd.returnOutput()
        }
        return newCmd
    }

    MavenCommand returnOutput() {
        this.returnStdout = true
        return this
    }

    MavenCommand printSettings() {
        this.settingsConfig.printSettings()
        return this
    }

}
