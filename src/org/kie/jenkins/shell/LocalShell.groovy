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

    LocalShell(def script, String installationDir = Utils.createTempDir(script), String cpuArchitecture = 'amd64') {
        super(script, installationDir, cpuArchitecture)
    }

    @Override
    String getFullCommand(String command) {
        return """
            export PATH=\${PATH}:${installations.collect { it.getBinaryPaths().join(':') }.join(':')}
            ${getEnvironmentVariables().collect { key, value -> "export ${key}=${value}" }.join('\n')}
            ${command}
        """
    }

}
