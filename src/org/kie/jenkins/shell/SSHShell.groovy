package org.kie.jenkins.shell

import org.kie.jenkins.Utils

/**
 * SSHShell allow to manage local installations of binaries on a remote system via SSH.
 *
 * Use `install` method to add new binaries to local shell.
 *
 * All commands using local binaries should be done via the `execute*` methods.
 *
 * Example:
 *     SSHShell sshShell = SSHShell.create(this).install(new OpenshiftClientInstallation(this))
 *     sshShell.execute('oc version --client')
*/
class SSHShell extends LocalShell {

    String sshServer
    String sshOptions

    SSHShell(def script, String sshServer, String sshOptions = '', String installationDir = '', String cpuArchitecture = '') {
        super(script, installationDir, cpuArchitecture)
        this.sshServer = sshServer
        this.sshOptions = sshOptions
    }

    @Override
    String getFullCommand(String command, String directory) {
        String shellCommand = super.getFullCommand(command, directory)

        return "ssh ${sshOptions} ${sshServer} \"${shellCommand}\""
    }

}
