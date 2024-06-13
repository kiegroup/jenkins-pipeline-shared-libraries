package org.kie.jenkins.shell.installation

abstract class AbstractInstallation implements Installation {

    def script

    public String installedVersion

    public String cpuArchitecture = ''

    List binaryPaths = []

    boolean debug = false

    AbstractInstallation(def script, String installedVersion) {
        this.script = script
        this.installedVersion = installedVersion
    }

    @Override
    void install(String installDir) {
        this.binaryPaths.addAll(this.installInDir(installDir))
    }

    /**
    * Should return a list of binary paths to add
    **/
    abstract List installInDir(String installDir)

    @Override
    void enableDebug() {
        this.debug = true
    }

    def getJenkinsScript() {
        return this.script
    }

    @Override
    void setCpuArchitecture(String cpuArchitecture) {
        this.cpuArchitecture = cpuArchitecture
    }
}
