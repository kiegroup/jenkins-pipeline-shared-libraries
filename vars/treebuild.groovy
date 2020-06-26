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
    Map<String, List<String>> projectGoalsMap = [:]
    projectCollection.each {
        projectGoalsMap.put(it, projectGoalsMap.get(it) ? projectGoalsMap.get(it).plus(goals) : [goals])
        // `projectGoalsMap[it] << goals` it's not working in our Jenkins instance
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

    def alreadyExistingProjects = getAlreadyExistingUpperLevelProjects(projectGoalsMap, settingsXmlId)
    projectCollection.removeAll(alreadyExistingProjects)
    println "Upstream building. Already existing projects [${alreadyExistingProjects}]. Final collection to build [${projectCollection}]"

    // Build project tree from currentProject node
    for (i = 0; i == 0 || currentProject != projectCollection.get(i - 1); i++) {
        projectGoalsMap[projectCollection.get(i)].each {
            util.buildProject(projectCollection.get(i), settingsXmlId, it, skipTests)
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
    Map<String, List<String>> projectGoalsMap = [:]
    projectCollection.each {
        projectGoalsMap.put(it, projectGoalsMap.get(it) ? projectGoalsMap.get(it).plus(goals) : [goals])
        // `projectGoalsMap[it] << goals` it's not working in our Jenkins instance
    }
    upstreamBuild(projectGoalsMap, currentProject, settingsXmlId, skipTests)
}

/**
 * Removes the already existing projects in maven repository which has no branch as the one from the change
 * @param projectGoalsMap the map with project as key and different maven goals per project
 * @param settingsXmlId maven settings xml file id
 * @return the new projectGoalsMap with the removed projects
 */
def getAlreadyExistingUpperLevelProjects(Map<String, List<String>> projectGoalsMap, String settingsXmlId) {
    List<String> result = []
    def projectTriggeringJob = util.getProjectTriggeringJob()[1]
    def currentProjectIndex = projectGoalsMap.findIndexOf { it.key == projectTriggeringJob }
    def projectCollection = projectGoalsMap.keySet().collect()

    for (i = 0; i < currentProjectIndex; i++) {
        def key = projectCollection.get(i)
        def projectGroupName = util.getProjectGroupName(key)
        def group = projectGroupName[0]
        def name = projectGroupName[1]
        if (maven.artifactExists(settingsXmlId, maven.getPomArtifact("${env.WORKSPACE}/${group}_${name}/pom.xml")) && !hasProjectChangingBranch(key)) {
            result.add(key)
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
