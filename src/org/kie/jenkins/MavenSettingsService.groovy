package org.kie.jenkins

class MavenSettingsService {

    def steps

    MavenSettingsConfig mavenSettingsConfig

    MavenSettingsService(def steps) {
        this(steps, new MavenSettingsConfig(steps))
    }

    MavenSettingsService(def steps, MavenSettingsConfig mavenSettingsConfig) {
        this.steps = steps
        this.mavenSettingsConfig = mavenSettingsConfig
    }

    String createSettingsFile() {
        String settingsFile = this.mavenSettingsConfig.settingsXmlPath
        if (this.mavenSettingsConfig.settingsXmlConfigFileId) {
            steps.configFileProvider([steps.configFile(fileId: this.mavenSettingsConfig.settingsXmlConfigFileId, targetLocation: 'maven-settings.xml', variable: 'MAVEN_SETTINGS_XML')]) {
                settingsFile = steps.env['MAVEN_SETTINGS_XML']
            }
        }
        if (settingsFile) {
            this.mavenSettingsConfig.dependenciesRepositoriesInSettings.each { MavenSettingsUtils.setRepositoryInSettings(steps, settingsFile, it.key, it.value) }

            this.mavenSettingsConfig.disabledMirrorRepoInSettings.each {
                MavenSettingsUtils.disableMirrorForRepoInSettings(steps, settingsFile, it)
            }

            if (this.mavenSettingsConfig.disableSnapshotsInSettings) {
                MavenSettingsUtils.disableSnapshotsInSettings(steps, settingsFile)
            }
            return settingsFile
        } else {
            return ''
        }
    }

}
