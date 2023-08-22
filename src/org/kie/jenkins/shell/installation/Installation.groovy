package org.kie.jenkins.shell.installation

interface Installation {

    void install(String installDir)

    List getBinaryPaths()

    void enableDebug()

    Map getExtraEnvVars()

}
