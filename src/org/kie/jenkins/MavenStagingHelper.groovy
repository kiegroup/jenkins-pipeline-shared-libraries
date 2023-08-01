package org.kie.jenkins

/**
* Helper to stage and promote artifacts to Nexus
* 
* Prerequisites: Your artifacts should be deployed locally before using this helper
*
* Actions:
* 1 - stageLocalArtifacts will create a staging repository and close it once done so your artifacts can be accessible.
*   In the return properties, you can find the staging properties (id and link).
* 2 - promoteStagingRepository will promote a given staging repository to a build promotion profile.
*   This is useful if you have many staging repositories, so that all of them can be accessible in one URL.
* 
* More information
*  - https://help.sonatype.com/repomanager2/staging-releases
*  - https://github.com/sonatype/nexus-maven-plugins/tree/master/staging/maven-plugin#deploy-staged-repository
*/
def class MavenStagingHelper {

    static String nexusPluginGroupId = 'org.sonatype.plugins'
    static String nexusStagingPluginArtifactId = 'nexus-staging-maven-plugin'
    static String nexusStagingPluginVersion = '1.6.13'

    def steps

    MavenCommand mvnCommand

    String nexusReleaseUrl = 'https://repository.jboss.org/nexus'
    String nexusReleaseRepositoryId = 'jboss-releases-repository'

    // If not defined, will retrieve default from project artifactId & version
    String stagingDescription

    // Will be filled once `stageLocalArtifacts` is called or via the `withStagingRepositoryId` method
    String stagingRepositoryId

    MavenStagingHelper(steps){
        this(steps, new MavenCommand(steps))
    }

    MavenStagingHelper(steps, mvnCommand){
        this.steps = steps
        this.mvnCommand = mvnCommand
    }

    /**
    * Use this method to stage artifacts that have been deployed locally (in given `localArtifactsFolder` parameter)
    * It returns a Map of staging properties
    */
    Map stageLocalArtifacts(String stagingProfileId, String localArtifactsFolder){
        assert stagingProfileId: 'Please provide a stagingProfileId'
        assert localArtifactsFolder: 'Please provide a local folder where artifacts are stored'
        
        steps.println "[INFO] Staging artifacts to staging profile ID ${stagingProfileId}"

        getDefaultMavenCommand()
            .withProperty('keepStagingRepositoryOnCloseRuleFailure', true)
            .withProperty('stagingProgressTimeoutMinutes', 10)
            .withProperty('repositoryDirectory', localArtifactsFolder)
            .withProperty('stagingProfileId', stagingProfileId)
            .run(getNexusStagingRunCommand('deploy-staged-repository'))

        // Retrieve `stagingRepositoryId` and fill it
        Map stagingProps = retrieveStagingProperties(localArtifactsFolder)
        withStagingRepositoryId(stagingProps['stagingRepository.id'])
        
        return stagingProps
    }

    /**
    * Use this method to promote a staging repository to a specific build promotion profile.
    * Note that if you did not execute `stageLocalArtifacts` first, you will need to provide the staging repository id via `withStagingRepositoryId`
    */
    def promoteStagingRepository(String buildPromotionProfileId) {
        assert this.stagingRepositoryId: 'Please provide a stagingRepositoryId via staging local artifacts or via withStagingRepositoryId method'
        assert buildPromotionProfileId: 'Please provide a buildPromotionProfileId'

        steps.println "[INFO] Promote artifacts from staging repository ${this.stagingRepositoryId} to build promotion profile ID ${buildPromotionProfileId}"

        getDefaultMavenCommand()
            .withProperty('buildPromotionProfileId', buildPromotionProfileId)
            .withProperty('stagingRepositoryId', this.stagingRepositoryId)
            .run(getNexusStagingRunCommand('promote'))
    }

    private MavenCommand getDefaultMavenCommand(){
        String projectName = getProjectArtifactId()
        String projectVersion = getProjectVersion()
        String description =  this.stagingDescription ?: "${projectName} ${projectVersion}"

        return this.mvnCommand.clone()
            .withOptions(["--projects :${projectName}"])
            .withProperty('nexusUrl', this.nexusReleaseUrl)
            .withProperty('serverId', this.nexusReleaseRepositoryId)
            .withProperty('stagingDescription', "'${description}'")
    }

    MavenStagingHelper withNexusReleaseUrl(String nexusReleaseUrl) {
        this.nexusReleaseUrl = nexusReleaseUrl
        return this
    }

    MavenStagingHelper withNexusReleaseRepositoryId(String nexusReleaseRepositoryId) {
        this.nexusReleaseRepositoryId = nexusReleaseRepositoryId
        return this
    }

    MavenStagingHelper withStagingDescription(String stagingDescription) {
        this.stagingDescription = stagingDescription
        return this
    }

    MavenStagingHelper withStagingRepositoryId(String stagingRepositoryId) {
        this.stagingRepositoryId = stagingRepositoryId
        return this
    }

    private Properties retrieveStagingProperties(String folder){
        String repositoryPropsFile = steps.sh(script: "find ${folder} -name *.properties", returnStdout: true).trim()
        steps.println "[INFO] Got staging properties file ${repositoryPropsFile}"
        assert repositoryPropsFile: 'No staging properties file has been found'
        
        return steps.readProperties(file: repositoryPropsFile)
    }

    private String getProjectArtifactId() {
        return executeMavenHelpEvaluateCommand('project.artifactId')
    }
    private String getProjectVersion() {
        return executeMavenHelpEvaluateCommand('project.version')
    }
    private String executeMavenHelpEvaluateCommand(String expression){
        return this.mvnCommand.clone()
            .withOptions(['-q'])
            .withProperty('expression', expression)
            .withProperty('forceStdout')
            .returnOutput()
            .run('help:evaluate')
            .trim()
    }

    private static String getNexusStagingRunCommand(String target){
        return "${nexusPluginGroupId}:${nexusStagingPluginArtifactId}:${nexusStagingPluginVersion}:${target}"
    }

}
