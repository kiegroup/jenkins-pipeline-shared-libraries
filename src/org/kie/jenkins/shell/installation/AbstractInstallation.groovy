package org.kie.jenkins.shell.installation

abstract class AbstractInstallation implements Installation {

    def script

    public String installedVersion

    public String cpuArchitecture = 'amd64'

    List binaryPaths = []

    boolean debug = false

    AbstractInstallation(def script, String installedVersion) {
        this.script = script
        this.installedVersion = installedVersion
    }

    void install(String installDir) {
        this.binaryPaths.addAll(this.installInDir(installDir))
    }

    /**
    * Should return a list of binary paths to add
    **/
    abstract List installInDir(String installDir)

    void enableDebug() {
        this.debug = true
    }

    def getJenkinsScript() {
        return this.script
    }

    void setCpuArchitecture(String cpuArchitecture) {
        this.cpuArchitecture = cpuArchitecture
    }
}
