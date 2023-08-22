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

    AbstractShell(def script, String installationDir, String cpuArchitecture = 'amd64') {
        this.script = script
        this.installationDir = installationDir
        this.script = script
        this.cpuArchitecture = cpuArchitecture
    }

    @Override
    void enableDebug() {
        this.installations.each { installation -> installation.enableDebug() }
    }

    @Override
    void install(Installation installation) {
        installation.setCpuArchitecture(this.cpuArchitecture)
        installation.install(this.installationDir)
        this.installations.add(installation)
    }

    @Override
    void execute(String command) {
        this.script.sh(getFullCommand(command))
    }

    @Override
    String executeWithOutput(String command) {
        return this.script.sh(script: getFullCommand(command), returnStdout: true).trim()
    }

    @Override
    def executeWithStatus(String command) {
        return this.script.sh(script: getFullCommand(command), returnStatus: true)
    }

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

}
