package org.kie.jenkins

class MavenSettingsConfigBuilder {

    String settingsXmlConfigFileId = ''
    String settingsXmlPath = ''
    Map dependenciesRepositoriesInSettings = [:]
    Set disabledMirrorRepoInSettings = []
    boolean disableSnapshotsInSettings = false
    List servers = []

    static MavenSettingsConfigBuilder from(MavenSettingsConfig settingsConfig) {
        return new MavenSettingsConfigBuilder()
            .settingsXmlConfigFileId(settingsConfig.settingsXmlConfigFileId)
            .settingsXmlPath(settingsConfig.settingsXmlPath)
            .dependenciesRepositoriesInSettings(settingsConfig.dependenciesRepositoriesInSettings)
            .disabledMirrorRepoInSettings(settingsConfig.disabledMirrorRepoInSettings)
            .disableSnapshotsInSettings(settingsConfig.disableSnapshotsInSettings)
            .servers(settingsConfig.servers)
    }

    MavenSettingsConfig build() {
        MavenSettingsConfig mavenSettingsConfig = new MavenSettingsConfig()
        mavenSettingsConfig.setSettingsXmlConfigFileId(this.settingsXmlConfigFileId)
        mavenSettingsConfig.setSettingsXmlPath(this.settingsXmlPath)
        mavenSettingsConfig.setDependenciesRepositoriesInSettings(this.dependenciesRepositoriesInSettings)
        mavenSettingsConfig.setDisabledMirrorRepoInSettings(this.disabledMirrorRepoInSettings)
        mavenSettingsConfig.setDisableSnapshotsInSettings(this.disableSnapshotsInSettings)
        mavenSettingsConfig.setServers(this.servers)
        return mavenSettingsConfig
    }

    MavenSettingsConfigBuilder clone() {
        return MavenSettingsConfigBuilder.from(this.build())
    }

    MavenSettingsConfigBuilder settingsXmlConfigFileId(String settingsXmlConfigFileId) {
        this.settingsXmlConfigFileId = settingsXmlConfigFileId
        return this
    }

    MavenSettingsConfigBuilder settingsXmlPath(String settingsXmlPath) {
        this.settingsXmlPath = settingsXmlPath
        return this
    }

    MavenSettingsConfigBuilder dependenciesRepositoriesInSettings(Map dependenciesRepositoriesInSettings) {
        this.dependenciesRepositoriesInSettings.putAll(dependenciesRepositoriesInSettings)
        return this
    }

    MavenSettingsConfigBuilder disabledMirrorRepoInSettings(Set disabledMirrorRepoInSettings) {
        this.disabledMirrorRepoInSettings.addAll(disabledMirrorRepoInSettings)
        return this
    }

    MavenSettingsConfigBuilder disableSnapshotsInSettings(boolean disableSnapshotsInSettings) {
        this.disableSnapshotsInSettings = disableSnapshotsInSettings
        return this
    }

    MavenSettingsConfigBuilder servers(List servers) {
        this.servers.addAll(servers)
        return this
    }

}
