package org.kie.jenkins.shell.installation

/**
 * Golang binaries local installation. Recommended to use with org.kie.jenkins.shell.LocalShell class.
 *
 * Binaries will be installed from https://golang.org/dl/ repository.
 *
 * This installation will also set GOENV, GOPATH and GOCACHE environement variables.
 */
class GolangInstallation extends AbstractInstallation {

    String goPath = ''
    String goEnv = ''
    String goCache = ''

    GolangInstallation(def script, String installedVersion) {
        super(script, installedVersion)
    }

    List installInDir(String installDir) {
        getJenkinsScript().dir(installDir) {
            getJenkinsScript().sh """
                wget --no-verbose https://golang.org/dl/go${installedVersion}.linux-${cpuArchitecture}.tar.gz
                tar -xzf go${installedVersion}.linux-${cpuArchitecture}.tar.gz
            """
        }

        setupExtraPaths(installDir)

        String golangBinDir = "${installDir}/go/bin"
        getJenkinsScript().echo "Golang installed in path '${golangBinDir}'"
        return [ golangBinDir, "${this.goPath}/bin" ]
    }

    void setupExtraPaths(String installDir) {
        this.goPath = "${installDir}/gopath"
        this.getJenkinsScript().sh "mkdir -p ${this.goPath}"

        this.goEnv = "${installDir}/goenv"
        this.getJenkinsScript().sh "mkdir -p ${this.goEnv}"

        this.goCache = "${installDir}/gocache"
        this.getJenkinsScript().sh "mkdir -p ${this.goCache}"
    }

    Map getExtraEnvVars() {
        return [
            'GOPATH': this.goPath,
            'GOENV': this.goEnv,
            'GOCACHE': this.goCache
        ]
    }

}
