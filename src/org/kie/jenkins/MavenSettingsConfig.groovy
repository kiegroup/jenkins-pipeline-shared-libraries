package org.kie.jenkins

import groovy.transform.builder.Builder
import groovy.transform.AutoClone

@AutoClone
class MavenSettingsConfig {

    /**
    * If set, override `withSettingsXmlFile`
    **/
    String settingsXmlConfigFileId = ''
    String settingsXmlPath = ''
    Map dependenciesRepositoriesInSettings = [:]
    Set disabledMirrorRepoInSettings = []
    boolean disableSnapshotsInSettings = false
}
