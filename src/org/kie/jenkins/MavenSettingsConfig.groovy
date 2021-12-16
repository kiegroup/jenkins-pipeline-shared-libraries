package org.kie.jenkins

class MavenSettingsConfig {

    def steps

    String settingsXmlConfigFileId = ''
    String settingsXmlPath = ''
    Map dependenciesRepositoriesInSettings = [:]
    List disabledMirrorRepoInSettings = []
    boolean disableSnapshotsInSettings = false

    boolean printSettings = false

    MavenSettingsConfig(steps) {
        this.steps = steps
    }

    /**
    * IF set, override `withSettingsXmlFile`
    **/
    MavenSettingsConfig withSettingsXmlId(String settingsXmlId) {
        this.settingsXmlConfigFileId = settingsXmlId
        return this
    }

    MavenSettingsConfig withSettingsXmlFile(String settingsXmlPath) {
        assert settingsXmlPath: 'Trying to set an empty settings xml path'
        this.settingsXmlPath = settingsXmlPath
        return this
    }

    MavenSettingsConfig withDependencyRepositoryInSettings(String repoId, String repoUrl) {
        this.dependenciesRepositoriesInSettings.put(repoId, repoUrl)
        return this
    }

    MavenSettingsConfig withDependencyRepositoriesInSettings(Map repositories = [:]) {
        this.dependenciesRepositoriesInSettings.putAll(repositories)
        return this
    }

    MavenSettingsConfig withMirrorDisabledForRepoInSettings(String repoId) {
        if (!this.disabledMirrorRepoInSettings.find { it == repoId } ) {
            this.disabledMirrorRepoInSettings.add(repoId)
    }
        return this
}

    MavenSettingsConfig withSnapshotsDisabledInSettings() {
        this.disableSnapshotsInSettings = true
        return this
    }

    MavenSettingsConfig printSettings() {
        this.printSettings = true
        return this
    }

    MavenSettingsConfig clone() {
        def newCfg = new MavenSettingsConfig(this.steps)
            .withDependencyRepositoriesInSettings(this.dependenciesRepositoriesInSettings)
        if (this.settingsXmlConfigFileId) {
            newCfg.withSettingsXmlId(this.settingsXmlConfigFileId)
        }
        this.disabledMirrorRepoInSettings.each {
            newCfg.withMirrorDisabledForRepoInSettings(it)
        }
        if (this.disableSnapshotsInSettings) {
            newCfg.withSnapshotsDisabledInSettings()
        }
        if (this.settingsXmlPath) {
            newCfg.withSettingsXmlFile(this.settingsXmlPath)
        }
        return newCfg
    }

}
