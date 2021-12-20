package org.kie.jenkins

import groovy.transform.builder.Builder
import groovy.transform.builder.ExternalStrategy
import groovy.transform.AutoClone

@Builder(builderStrategy=ExternalStrategy, forClass=MavenSettingsConfig)
@AutoClone
class MavenSettingsConfigBuilder {

    static MavenSettingsConfigBuilder from(MavenSettingsConfig settingsConfig) {
        return new MavenSettingsConfigBuilder()
      .settingsXmlConfigFileId(settingsConfig.settingsXmlConfigFileId)
      .settingsXmlPath(settingsConfig.settingsXmlPath)
      .dependenciesRepositoriesInSettings(settingsConfig.dependenciesRepositoriesInSettings)
      .disabledMirrorRepoInSettings(settingsConfig.disabledMirrorRepoInSettings)
      .disableSnapshotsInSettings(settingsConfig.disableSnapshotsInSettings)
    }

}
