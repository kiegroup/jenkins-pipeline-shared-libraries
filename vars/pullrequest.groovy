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

    treebuild.checkoutProjects(projectCollection, currentProject)

    // Build project tree from currentProject node
    for (i = 0; currentProject != projectCollection.get(i); i++) {
        println "Current Upstream Project:" + projectCollection.get(i)
        treebuild.buildProject(projectCollection.get(i), settingsXmlId, treebuild.getGoals(projectCollection.get(i), propertiesFileId, 'upstream'))
    }

    println "Build of current project: ${currentProject}"
    treebuild.buildProject(currentProject, settingsXmlId, treebuild.getGoals(currentProject, propertiesFileId))

    if(sonarCloudReps.contains(currentProject)) {
        println "SONARCLOUD analysis of : ${currentProject}"
        buildSonar(currentProject, settingsXmlId, treebuild.getGoals(currentProject, propertiesFileId, 'sonarcloud'), sonarCloudId)
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
    def projectGroupName = treebuild.getProjectGroupName(project)
    def group = projectGroupName[0]
    def name = projectGroupName[1]

    println "Building ${group}/${name}"
    dir("${env.WORKSPACE}/${group}_${name}") {
        maven.runMavenWithSettingsSonar(settingsXmlId, goals, sonarCloudId, "${group}_${name}.maven.log")
    }
}

return this;