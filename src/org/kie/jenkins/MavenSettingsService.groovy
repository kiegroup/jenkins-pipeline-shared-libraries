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
            this.mavenSettingsConfig.dependenciesRepositoriesInSettings.each { setRepositoryInSettings(settingsFile, it.key, it.value) }

            this.mavenSettingsConfig.disabledMirrorRepoInSettings.each {
                disableMirrorForRepoInSettings(settingsFile, it)
            }

            if (this.mavenSettingsConfig.disableSnapshotsInSettings) {
                steps.sh "sed -i '/<repository>/,/<\\/repository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' ${settingsFile}"
                steps.sh "sed -i '/<pluginRepository>/,/<\\/pluginRepository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' ${settingsFile}"
            }

            if (this.mavenSettingsConfig.printSettings) {
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
