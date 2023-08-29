package org.kie.jenkins.shell

import org.kie.jenkins.shell.installation.Installation
import org.kie.jenkins.Utils

/**
 * Common methods for a shell
*/
abstract class AbstractShell implements Shell {

    def script
    String installationDir
    String cpuArchitecture
    List<Installation> installations = []
    Map envVars = [:]
    boolean debug = false

    AbstractShell(def script, String installationDir = '', String cpuArchitecture = '') {
        this.script = script
        this.installationDir = installationDir
        this.cpuArchitecture = cpuArchitecture ?: 'amd64'
    }

    @Override
    void enableDebug() {
        this.debug = true
        this.installations.each { installation -> installation.enableDebug() }
    }

    @Override
    void install(Installation installation) {
        installation.setCpuArchitecture(this.cpuArchitecture)
        installation.install(getInstallationDir())
        if (debug) {
            installation.enableDebug()
        }
        this.installations.add(installation)
    }

    @Override
    void execute(String command) {
        execute(command, '')
    }

    @Override
    void execute(String command, String directory) {
        String fullCommand = getFullCommand(command, directory)
        if (debug) {
            println "[DEBUG] Run command: ${fullCommand}"
        }
        this.script.sh(fullCommand)
    }

    @Override
    String executeWithOutput(String command) {
        return executeWithOutput(command, '')
    }

    @Override
    String executeWithOutput(String command, String directory) {
        String fullCommand = getFullCommand(command, directory)
        if (debug) {
            println "[DEBUG] Run command: ${fullCommand}"
        }
        return this.script.sh(returnStdout: true, script: fullCommand).trim()
    }

    @Override
    def executeWithStatus(String command) {
        return executeWithStatus(command, '')
    }

    @Override
    def executeWithStatus(String command, String directory) {
        String fullCommand = getFullCommand(command, directory)
        if (debug) {
            println "[DEBUG] Run command: ${fullCommand}"
        }
        return this.script.sh(returnStatus: true, script: fullCommand)
    }

    String getFullCommand(String command) {
        return getFullCommand(command, '')
    }
    /*
    * Return the full text command with additions for the shell
    */
    abstract String getFullCommand(String command, String directory)

    @Override
    void addEnvironmentVariable(String key, String value) {
        this.envVars.put(key, value)
    }

    @Override
    Map getEnvironmentVariables() {
        Map vars = [:]
        this.installations.each { installation ->
            vars.putAll(installation.getExtraEnvVars())
        }
        vars.putAll(this.envVars)
        return vars
    }

    String getInstallationDir() {
        if (!this.installationDir) {
            // Cannot put this in constructor due to https://www.jenkins.io/doc/book/pipeline/cps-method-mismatches/
            this.installationDir = Utils.createTempDir(script)
        }
        return this.installationDir
    }

    def getJenkinsScript() {
        return this.script
    }
}
