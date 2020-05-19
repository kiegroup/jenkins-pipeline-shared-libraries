/**
 * Checks out the project collection
 *
 * @param projectCollection the list of projects to be checked out
 * @param limitProject the project to stop
 */
def checkoutProjects(List<String> projectCollection, String limitProject = null) {
    println "Checking out projects ${projectCollection}"

    for (i = 0; limitProject ? (i == 0 || limitProject != projectCollection.get(i-1)) : i < projectCollection.size(); i++) {
        def projectGroupName = getProjectGroupName(projectCollection.get(i))
        def group = projectGroupName[0]
        def name = projectGroupName[1]
        sh "mkdir -p ${group}_${name}"
        dir("${env.WORKSPACE}/${group}_${name}") {
            checkoutProject(name, group)
        }
    }
}

/**
 *
 * Checks out the repo
 *
 * @param name project repo name
 * @param group project group
 */
def checkoutProject(String name, String group) {
    def changeAuthor = env.CHANGE_AUTHOR ?: ghprbPullAuthorLogin
    def changeBranch = env.CHANGE_BRANCH ?: ghprbSourceBranch
    def changeTarget = env.CHANGE_TARGET ?: ghprbTargetBranch
    println "Checking out author [${changeAuthor}] branch [${changeBranch}] target [${changeTarget}]"
    githubscm.checkoutIfExists(name, "$changeAuthor", "$changeBranch", group, "$changeTarget")
}

/**
 *
 * @param projectUrl the github project url
 */
def getProject(String projectUrl) {
    return (projectUrl =~ /((git|ssh|http(s)?)|(git@[\w\.]+))(:(\/\/)?(github.com\\/))([\w\.@\:\/\-~]+)(\.git)(\/)?/)[0][8]
}

/**
 * Returns an array containing group and name
 *
 * @param project the project
 * @param defaultGroup the default project group. Optional.
 */
def getProjectGroupName(String project, String defaultGroup = "kiegroup") {
    def projectNameGroup = project.split("\\/")
    def group = projectNameGroup.size() > 1 ? projectNameGroup[0] : defaultGroup
    def name = projectNameGroup.size() > 1 ? projectNameGroup[1] : project
    return [group, name]
}

/**
 *
 * @param project a string following the pattern PROJECT_GROUP/PROJECT_NAME, for example kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param goals maven goals
 * @param skipTests Boolean to skip tests or not
 */
def buildProject(String project, String settingsXmlId, String goals, Boolean skipTests = null) {
    def projectGroupName = getProjectGroupName(project)
    def group = projectGroupName[0]
    def name = projectGroupName[1]

    println "Building ${group}/${name}"
    dir("${env.WORKSPACE}/${group}_${name}") {
        maven.runMavenWithSettings(settingsXmlId, goals, skipTests != null ? skipTests : new Properties(), "${group}_${name}.maven.log")
    }
}

/**
 * Fetches the goals from a Jenkins properties file
 * @param project - current project
 * @param propertiesFileId - pointing to a Jenkins properties file
 * @param type (can be "current" or "upstream")
 */
def getGoals(String project, String propertiesFileId, String type = 'current') {
    configFileProvider([configFile(fileId: propertiesFileId, variable: 'PROPERTIES_FILE')]) {
        def propertiesFile = readProperties file: PROPERTIES_FILE
        return propertiesFile."goals.${project}.${type}" ?: propertiesFile."goals.default.${type}"
    }
}