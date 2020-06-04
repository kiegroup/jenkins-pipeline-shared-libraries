/**
 * Builds the downstream
 * @param the file with a collection of items following the pattern PROJECT_GROUP/PROJECT_NAME, for example kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param goals maven goals
 * @param skipTests Boolean to skip tests or not
 */
def build(List<String> projectCollection, String settingsXmlId, String goals, Boolean skipTests = null) {
    println "Downstream building. Reading Lines for ${projectCollection}"
    def lastLine = projectCollection.get(projectCollection.size() - 1)
    println "Downstream building ${lastLine} project."
    upstreamBuild(projectCollection, lastLine, settingsXmlId, goals, skipTests)
}

/**
 * Builds the upstream for an specific project
 * @param the file with a collection of items following the pattern PROJECT_GROUP/PROJECT_NAME, for example kiegroup/drools
 * @param currentProject the project to build the stream from, like kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param goals maven goals
 * @param skipTests Boolean to skip tests or not
 */
def upstreamBuild(List<String> projectCollection, String currentProject, String settingsXmlId, String goals, Boolean skipTests = null) {
    println "Upstream building ${currentProject} project for ${projectCollection}"

    util.checkoutProjects(projectCollection, currentProject)

    // Build project tree from currentProject node
    for (i = 0; i == 0 || currentProject != projectCollection.get(i-1); i++) {
        util.buildProject(projectCollection.get(i), settingsXmlId, goals, skipTests)
    }
}

