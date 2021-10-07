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

    String createSettingsFile() {
        String settingsFile = this.settingsXmlPath
        if (this.settingsXmlConfigFileId) {
            steps.configFileProvider([steps.configFile(fileId: this.settingsXmlConfigFileId, targetLocation: 'maven-settings.xml', variable: 'MAVEN_SETTINGS_XML')]) {
                settingsFile = steps.env['MAVEN_SETTINGS_XML']
            }
        }
        if (settingsFile) {
            this.dependenciesRepositoriesInSettings.each { setRepositoryInSettings(settingsFile, it.key, it.value) }

            this.disabledMirrorRepoInSettings.each {
                disableMirrorForRepoInSettings(settingsFile, it)
            }

            if (this.disableSnapshotsInSettings) {
                steps.sh "sed -i '/<repository>/,/<\\/repository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' ${settingsFile}"
                steps.sh "sed -i '/<pluginRepository>/,/<\\/pluginRepository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' ${settingsFile}"
            }

            if (this.printSettings) {
                steps.sh "cat ${settingsFile}"
            }
            return settingsFile
        } else {
            return ''
        }
    }

    private void setRepositoryInSettings(String settingsFilePath, String repoId, String repoUrl) {
        def depsRepositoryContent = "<id>${repoId}</id><name>${repoId}</name><url>${repoUrl}</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases>"
        steps.sh """
            sed -i 's|<repositories>|<repositories><!-- BEGIN added repository --><repository>${depsRepositoryContent}</repository><!-- END added repository -->|g' ${settingsFilePath}
            sed -i 's|<pluginRepositories>|<pluginRepositories><!-- BEGIN added repository --><pluginRepository>${depsRepositoryContent}</pluginRepository><!-- END added repository -->|g' ${settingsFilePath}
        """
        disableMirrorForRepoInSettings(settingsFilePath, repoId)
    }

    private void disableMirrorForRepoInSettings(String settingsFilePath, String repoId) {
        steps.sh "sed -i 's|</mirrorOf>|,!${repoId}</mirrorOf>|g' ${settingsFilePath}"
    }

}
