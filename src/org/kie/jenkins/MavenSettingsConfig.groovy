package org.kie.jenkins

class MavenSettingsConfig {

    /**
    * If set, override `withSettingsXmlFile`
    **/
    String settingsXmlConfigFileId = ''
    String settingsXmlPath = ''
    Map dependenciesRepositoriesInSettings = [:]
    Set disabledMirrorRepoInSettings = []
    boolean disableSnapshotsInSettings = false

    MavenSettingsConfig clone() {
        MavenSettingsConfig mavenSettingsConfig = new MavenSettingsConfig()
        mavenSettingsConfig.setSettingsXmlConfigFileId(this.settingsXmlConfigFileId)
        mavenSettingsConfig.setSettingsXmlPath(this.settingsXmlPath)
        mavenSettingsConfig.setDependenciesRepositoriesInSettings(this.dependenciesRepositoriesInSettings)
        mavenSettingsConfig.setDisabledMirrorRepoInSettings(this.disabledMirrorRepoInSettings)
        mavenSettingsConfig.setDisableSnapshotsInSettings(this.disableSnapshotsInSettings)
        return mavenSettingsConfig
    }
}
