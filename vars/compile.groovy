/**
 * Builds the compile downstream (upstream projects, the current project and the downstream projects)
 * @param the file with a collection of items following the pattern PROJECT_GROUP/PROJECT_NAME, for example kiegroup/drools
 * @param currentProject the project to build the stream from, like kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param propertiesFilePath path to the file that defines the maven goals for each rep
 */
def build(List<String> projectCollection, String currentProject, String settingsXmlId, String propertiesFilePath) {
    assert projectCollection.contains(currentProject): "The project ${currentProject} is not in project collection ${projectCollection}. Please check flow configuration or pull request information"

    util.prepareEnvironment()
    println "Compile downstream build of project [${currentProject}] for project collection ${projectCollection}"
    util.checkoutProjects(projectCollection)

    def currentProjectIndex = projectCollection.findIndexOf { it == currentProject }
    // Build project tree from currentProject node
    for (i = 0; i < projectCollection.size(); i++) {
        def type = i < currentProjectIndex ? 'upstream' :  i == currentProjectIndex ? 'current' : 'downstream'
        println "Build of project [${projectCollection.get(i)}] with type [${type}]"
        util.buildProject(projectCollection.get(i), settingsXmlId, util.getGoals(projectCollection.get(i), propertiesFilePath, type))
    }
}