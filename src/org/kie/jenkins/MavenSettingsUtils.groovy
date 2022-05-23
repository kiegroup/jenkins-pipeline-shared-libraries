package org.kie.jenkins

class MavenSettingsUtils {

    static void setRepositoryInSettings(def steps, String settingsFilePath, String repoId, String repoUrl) {
        def depsRepositoryContent = "<id>${repoId}</id><name>${repoId}</name><url>${repoUrl}</url><layout>default</layout><snapshots><enabled>true</enabled></snapshots><releases><enabled>true</enabled></releases>"
        steps.sh """
            sed -i 's|<repositories>|<repositories><!-- BEGIN added repository --><repository>${depsRepositoryContent}</repository><!-- END added repository -->|g' ${settingsFilePath}
            sed -i 's|<pluginRepositories>|<pluginRepositories><!-- BEGIN added repository --><pluginRepository>${depsRepositoryContent}</pluginRepository><!-- END added repository -->|g' ${settingsFilePath}
        """
        disableMirrorForRepoInSettings(steps, settingsFilePath, repoId)
    }

    static void disableMirrorForRepoInSettings(def steps, String settingsFilePath, String repoId) {
        steps.sh "sed -i 's|</mirrorOf>|,!${repoId}</mirrorOf>|g' ${settingsFilePath}"
    }

    static void disableSnapshotsInSettings(def steps, String settingsFilePath) {
      steps.sh "sed -i '/<repository>/,/<\\/repository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' ${settingsFilePath}"
      steps.sh "sed -i '/<pluginRepository>/,/<\\/pluginRepository>/ { /<snapshots>/,/<\\/snapshots>/ { s|<enabled>true</enabled>|<enabled>false</enabled>|; }}' ${settingsFilePath}"
    }

}
