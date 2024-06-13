package org.kie.jenkins.shell.installation

/**
 * Openshift client local installation. Recommended to use with org.kie.jenkins.shell.LocalShell class.
 *
 * Binaries will be installed from https://mirror.openshift.com/pub/openshift-v4/clients/ocp/ repository.
 */
class OpenshiftClientInstallation extends AbstractInstallation {

    OpenshiftClientInstallation(def script, String installedVersion = 'stable') {
        super(script, installedVersion)
    }

    List installInDir(String installDir) {
        String ocDir = "${installDir}/openshiftclient"

        getJenkinsScript().dir(ocDir) {
            getJenkinsScript().sh """
                wget --no-verbose https://mirror.openshift.com/pub/openshift-v4/clients/ocp/${installedVersion}/openshift-client-linux.tar.gz
                tar -xzf openshift-client-linux.tar.gz
            """
        }

        getJenkinsScript().echo "Openshift client installed in path '${ocDir}'"
        getJenkinsScript().sh "ls -al ${ocDir}"
        return [ ocDir ]
    }

    Map getExtraEnvVars() {
        return [:]
    }

}
