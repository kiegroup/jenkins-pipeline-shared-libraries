/**
 * Builds the upstream for a specific project + the project itself (normal PR) + a SONARCLOUD analisys if needed
 * @param the file with a collection of items following the pattern PROJECT_GROUP/PROJECT_NAME, for example kiegroup/drools
 * @param currentProject the project to build the stream from, like kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param propertiesFileId file that defines the maven goals for each rep
 */
def build(List<String> projectCollection, String currentProject, String settingsXmlId, String propertiesFileId, String sonarCloudId, sonarCloudReps = []) {
    println "Building of project ${currentProject}"
    println "Project collection: ${projectCollection}"

    util.checkoutProjects(projectCollection, currentProject)


    def alreadyExistingProjects = getAlreadyExistingUpperLevelProjects(projectCollection, currentProject, settingsXmlId)
    projectCollection.removeAll(alreadyExistingProjects)
    println "Building. Already existing projects [${alreadyExistingProjects}]. Final collection to build [${projectCollection}]"

    // Build project tree from currentProject node
    for (i = 0; currentProject != projectCollection.get(i); i++) {
        println "Current Upstream Project:" + projectCollection.get(i)
        util.buildProject(projectCollection.get(i), settingsXmlId, util.getGoals(projectCollection.get(i), propertiesFileId, 'upstream'))
    }

    println "Build of current project: ${currentProject}"
    util.buildProject(currentProject, settingsXmlId, util.getGoals(currentProject, propertiesFileId))

    if(sonarCloudReps.contains(currentProject)) {
        println "SONARCLOUD analysis of : ${currentProject}"
        buildSonar(currentProject, settingsXmlId, util.getGoals(currentProject, propertiesFileId, 'sonarcloud'), sonarCloudId)
    } else {
        println "INFO: ${currentProject} project is not for SONARCLOUD analysis"
    }
}

/**
 *
 * @param project a string following the pattern PROJECT_GROUP/PROJECT_NAME, for example kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param goals maven goals
 * @param sonarCloudId token for sonarcloud
 */
def buildSonar(String project, String settingsXmlId, String goals, String sonarCloudId) {
    def projectGroupName = util.getProjectGroupName(project)
    def group = projectGroupName[0]
    def name = projectGroupName[1]

    println "Building ${group}/${name}"
    def dirPath = util.isProjectTriggeringJob(projectGroupName) == true ? "${env.WORKSPACE}" : "${env.WORKSPACE}/${group}_${name}"
    dir(dirPath) {
        maven.runMavenWithSettingsSonar(settingsXmlId, goals, sonarCloudId, "${group}_${name}.maven.log")
    }
}

/**
 * Removes the already existing projects in maven repository which has no branch as the one from the change
 * @param projectGoalsMap the map with project as key and different maven goals per project
 * @param settingsXmlId maven settings xml file id
 * @return the new projectGoalsMap with the removed projects
 */
def getAlreadyExistingUpperLevelProjects(List<String> projectCollection, String currentProject, String settingsXmlId) {
    List<String> result = []

    for (i = 0; currentProject != projectCollection.get(i); i++) {
        def project = projectCollection.get(i)
        def projectGroupName = util.getProjectGroupName(project)
        def group = projectGroupName[0]
        def name = projectGroupName[1]
        if (maven.artifactExists(settingsXmlId, maven.getPomArtifact("${env.WORKSPACE}/${group}_${name}/pom.xml")) && !hasProjectChangingBranch(project)) {
            result.add(project)
        }
    }
    return result
}

/**
 * Has the repository the changing branch for the changing author
 * @param repositoryName the name of the repository
 * @return is the project has a the changing branch for the changing author
 */
def hasProjectChangingBranch(String repositoryName) {
    String changeAuthor = env.CHANGE_AUTHOR ?: ghprbPullAuthorLogin
    String changeBranch = env.CHANGE_BRANCH ?: ghprbSourceBranch
    def result = githubscm.getRepositoryScm(repositoryName, changeAuthor, changeBranch)
    println "Has the project [${repositoryName}] a branch [${changeBranch}] for author [${changeAuthor}]. Result [${result != null}]"
    return result != null
}