package org.kie.jenkins.shell.installation

/**
 * CodeReadyContainers local installation. Recommended to use with org.kie.jenkins.shell.LocalShell class.
 *
 * Binaries will be installed from https://mirror.openshift.com/pub/openshift-v4/clients/crc/ repository.
 */
class CRCInstallation extends AbstractInstallation {

    CRCInstallation(def script, String installedVersion = 'latest') {
        super(script, installedVersion)
    }

    List installInDir(String installDir) {
        getJenkinsScript().dir(installDir) {
            getJenkinsScript().sh """
                wget --no-verbose https://mirror.openshift.com/pub/openshift-v4/clients/crc/${installedVersion}/crc-linux-${cpuArchitecture}.tar.xz
                tar -xf crc-linux-amd64.tar.xz
            """
        }

        String crcDir = getJenkinsScript().sh(returnStdout: true, script: "find ${installDir} -name 'crc-linux-*-${cpuArchitecture}' -type d").trim()
        getJenkinsScript().echo "CRC installed in path '${crcDir}'"
        getJenkinsScript().sh "ls -al ${crcDir}"
        return [ crcDir ]
    }

    Map getExtraEnvVars() {
        return [:]
    }

}
