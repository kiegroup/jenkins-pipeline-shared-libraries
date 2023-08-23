package org.kie.jenkins.shell

import org.kie.jenkins.shell.installation.Installation
import org.kie.jenkins.Utils

/**
 * LocalShell allow to manage local installations of binaries on the local system.
 *
 * Use `install` method to add new binaries to local shell.
 *
 * All commands using local binaries should be done via the `execute*` methods.
 *
 * Example:
 *     LocalShell localShell = new LocalShell(this).install(new OpenshiftClientInstallation(this))
 *     localShell.execute('oc version --client')
*/
class LocalShell extends AbstractShell {

    LocalShell(def script, String installationDir = '', String cpuArchitecture = '') {
        super(script, installationDir, cpuArchitecture)
    }

    @Override
    String getFullCommand(String command, String directory) {
        String fullCommand = ''
        if (directory) {
            fullCommand += "cd ${directory}"
            fullCommand += "\n"
        }
        if (installations) {
            fullCommand += "export PATH=\${PATH}:${installations.collect { it.getBinaryPaths().join(':') }.join(':')}"
            fullCommand += "\n"
        }
        Map envVars = getEnvironmentVariables()
        if (envVars) {
            fullCommand += envVars.collect { key, value -> "export ${key}=${value}" }.join('\n')
            fullCommand += "\n"
        }
        fullCommand += "${command}"
    }

}
