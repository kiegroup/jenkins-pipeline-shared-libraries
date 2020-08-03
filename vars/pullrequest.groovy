/**
 * Builds the upstream for a specific project + the project itself (normal PR) + a SONARCLOUD analysis if needed
 * @param the file with a collection of items following the pattern PROJECT_GROUP/PROJECT_NAME, for example kiegroup/drools
 * @param currentProject the project to build the stream from, like kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param propertiesFilePath path to the file that defines the maven goals for each rep
 */
def build(List<String> projectCollection, String currentProject, String settingsXmlId, String propertiesFilePath, String sonarCloudId, sonarCloudReps = []) {
    util.prepareEnvironment()
    println "Building of project ${currentProject}"
    println "Project collection: ${projectCollection}"

    util.checkoutProjects(projectCollection, currentProject)

    // Build project tree from currentProject node
    for (i = 0; currentProject != projectCollection.get(i); i++) {
        def project = projectCollection.get(i)
        println "Current Upstream Project: ${project}"
        util.buildProject(project, settingsXmlId, util.getGoals(project, propertiesFilePath, 'upstream'))

        // Once the project is built we should delete it in order not to interfere with sonar cloud analysis
        dir(util.getProjectDirPath(project)) {
            deleteDir()
        }
    }

    println "Build of current project: ${currentProject}"
    util.buildProject(currentProject, settingsXmlId, util.getGoals(currentProject, propertiesFilePath))

    if (sonarCloudReps.contains(currentProject)) {
        println "SONARCLOUD analysis of : ${currentProject}"
        buildSonar(currentProject, settingsXmlId, util.getGoals(currentProject, propertiesFilePath, 'sonarcloud'), sonarCloudId)
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
    dir("${env.WORKSPACE}") {
        maven.runMavenWithSettingsSonar(settingsXmlId, goals, sonarCloudId, "${group}_${name}.maven.log")
    }
}
