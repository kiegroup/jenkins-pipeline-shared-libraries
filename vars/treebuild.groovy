/**
 * Builds the downstream
 * @param projectGoalsMap the map with project as key and different maven goals per project
 * @param settingsXmlId maven settings xml file id
 * @param skipTests Boolean to skip tests or not
 */
 def build(Map<String, List<String>> projectGoalsMap, String settingsXmlId, Boolean skipTests = null) {
    println "Downstream building. Reading Lines for ${projectGoalsMap.keySet()}"
    def lastLine = projectGoalsMap.keySet().collect().get(projectGoalsMap.keySet().collect().size() - 1)
    println "Downstream building ${lastLine} project."
    upstreamBuild(projectGoalsMap, lastLine, settingsXmlId, skipTests)
}

/**
 * Builds the downstream
 * @param projectCollection collection of items following the pattern PROJECT_GROUP/PROJECT_NAME, for example kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param goals maven goals
 * @param skipTests Boolean to skip tests or not
 */
def build(List<String> projectCollection, String settingsXmlId, String goals, Boolean skipTests = null) {
    Map<String, List<String>> projectGoalsMap = [:].withDefault { [] }
    projectCollection.each {
        projectGoalsMap[it] << goals
    }
    build(projectGoalsMap, settingsXmlId, skipTests)
}

/**
 * Builds the upstream for an specific project
 * @param projectGoalsMap the map with project as key and different maven goals per project
 * @param currentProject the project to build the stream from, like kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param skipTests Boolean to skip tests or not
 */
def upstreamBuild(Map<String, List<String>> projectGoalsMap, String currentProject, String settingsXmlId, Boolean skipTests = null) {
    def projectCollection = projectGoalsMap.keySet().collect()
    println "Upstream building ${currentProject} project for ${projectCollection}"
    util.checkoutProjects(projectCollection, currentProject)

    // Build project tree from currentProject node
    for (i = 0; i == 0 || currentProject != projectCollection.get(i-1); i++) {
        projectGoalsMap[projectCollection.get(i)].each {
            util.buildProject(projectCollection.get(i), settingsXmlId, goals, skipTests)
        }
    }
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
    Map<String, List<String>> projectGoalsMap = [:].withDefault { [] }
    projectCollection.each {
        projectGoalsMap[it] << goals
    }
    upstreamBuild(projectGoalsMap, currentProject, settingsXmlId, skipTests)
}