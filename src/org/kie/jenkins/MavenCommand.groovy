package org.kie.jenkins

class MavenCommand {

    def steps

    String directory = ''
    boolean mavenWrapper = false

    MavenSettingsConfigBuilder settingsConfigBuilder

    List mavenOptions = []
    Map properties = [:]
    String logFileName = ''
    List profiles = []

    boolean returnStdout = false
    boolean printSettings = false

    MavenCommand(steps) {
        this.steps = steps
        settingsConfigBuilder = new MavenSettingsConfigBuilder()
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
        def cmdBuilder = new StringBuilder(this.mavenWrapper ? './mnw' : 'mvn')
        cmdBuilder.append(' -B')

        // Retrieve settings file from id if given
        String settingsFile = new MavenSettingsService(this.steps, this.settingsConfigBuilder.build()).createSettingsFile()
        if (settingsFile) {
            if (this.printSettings) {
                steps.sh "cat ${settingsFile}"
            }
            cmdBuilder.append(" -s ${settingsFile}")
        }

        if (this.mavenOptions.size()) {
            cmdBuilder.append(' ').append(this.mavenOptions.join(' '))
        }
        cmdBuilder.append(' ').append(goals)
        if (this.profiles.size()) {
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

    MavenCommand useMavenWrapper(boolean mavenWrapper = true) {
        this.mavenWrapper = mavenWrapper
        return this
    }

    /**
    * Overwrites all current settings config done.
    */
    MavenCommand withSettingsConfigBuilder(MavenSettingsConfigBuilder settingsConfigBuilder) {
        this.settingsConfigBuilder = settingsConfigBuilder
        return this
    }

    /**
    * IF set, override `withSettingsXmlFile`
    **/
    MavenCommand withSettingsXmlId(String settingsXmlId) {
        settingsConfigBuilder.settingsXmlConfigFileId(settingsXmlId)
        return this
    }

    MavenCommand withSettingsXmlFile(String settingsXmlPath) {
        assert settingsXmlPath: 'Trying to set an empty settings xml path'
        settingsConfigBuilder.settingsXmlPath(settingsXmlPath)
        return this
    }

    MavenCommand withDependencyRepositoryInSettings(String repoId, String repoUrl) {
        settingsConfigBuilder.dependenciesRepositoriesInSettings([(repoId) : repoUrl])
        withProperty('enforcer.skip', true)
        return this
    }

    MavenCommand withDependencyRepositoriesInSettings(Map repositories = [:]) {
        settingsConfigBuilder.dependenciesRepositoriesInSettings(repositories)
        withProperty('enforcer.skip', true)
        return this
    }

    MavenCommand withMirrorDisabledForRepoInSettings(String repoId) {
        settingsConfigBuilder.disabledMirrorRepoInSettings([repoId] as Set)
        return this
    }

    MavenCommand withSnapshotsDisabledInSettings(boolean disabled = true) {
        settingsConfigBuilder.disableSnapshotsInSettings(disabled)
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
        return this
    }

    MavenCommand withLocalDeployFolder(String localDeployFolder) {
        assert localDeployFolder: 'Trying to add an empty local deploy folder'
        withProperty('altDeploymentRepository', "local::default::file://${localDeployFolder}")
        return this
    }

    MavenCommand withServerInSettings(String serverId, String username, String password) {
        settingsConfigBuilder.servers([ [ id: serverId, username: username, password: password ] ])
        return this
    }

    MavenCommand clone() {
        def newCmd = new MavenCommand(this.steps)
            .withOptions(this.mavenOptions)
            .withPropertyMap(this.properties)
            .withProfiles(this.profiles)
            .withSettingsConfigBuilder(this.settingsConfigBuilder.clone())
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

    MavenCommand returnOutput(boolean returnStdout = true) {
        this.returnStdout = returnStdout
        return this
    }

    MavenCommand printSettings(boolean printSettings = true) {
        this.printSettings = printSettings
        return this
    }

}
