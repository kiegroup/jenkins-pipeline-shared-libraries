package org.kie.jenkins.shell.installation

/**
 * Groovy binary local installation. Recommended to use with org.kie.jenkins.shell.LocalShell class.
 *
 * Binaries will be installed from https://dl.bintray.com/groovy/maven/ repository.
 */
class GroovyInstallation extends AbstractInstallation {

    GroovyInstallation(def script, String installedVersion) {
        super(script, installedVersion)
    }

    List installInDir(String installDir) {
        getJenkinsScript().dir(installDir) {
            getJenkinsScript().sh """
                wget --no-verbose https://archive.apache.org/dist/groovy/${installedVersion}/distribution/apache-groovy-binary-${installedVersion}.zip
                unzip apache-groovy-binary-${installedVersion}.zip
            """
        }

        String groovyDir = "${installDir}/groovy-${installedVersion}/bin"
        getJenkinsScript().echo "Groovy installed in path '${groovyDir}'"
        getJenkinsScript().sh "ls -al ${groovyDir}"
        return [ groovyDir ]
    }

    Map getExtraEnvVars() {
        return [:]
    }
}
