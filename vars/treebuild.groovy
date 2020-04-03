/**
 * Builds the downstream
 * @param the file with a collection of items following the pattern PROJECT_GROUP/PROJECT_NAME, for example kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param goals maven goals
 * @param skipTests Boolean to skip tests or not
 */
def downstreamBuild(def projectCollection, String settingsXmlId, String goals, Boolean skipTests = null) {
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
def upstreamBuild(def projectCollection, String currentProject, String settingsXmlId, String goals, Boolean skipTests = null) {
    println "Upstream building ${currentProject} project for ${projectCollection}"
    // Build project tree from currentProject node
    for (i = 0; currentProject != projectCollection.get(i); i++) {
        buildProject(projectCollection.get(i), settingsXmlId, goals, skipTests)
    }

    buildProject(currentProject, settingsXmlId, goals, skipTests)
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
    sh "mkdir -p ${group}_${name}"
    dir("${env.WORKSPACE}/${group}_${name}") {
        checkoutProject(name, group)
        maven.runMavenWithSettings(settingsXmlId, goals, skipTests != null ? skipTests : new Properties())
    }
    sh "rm -rf ${group}_${name} 2> /dev/null"
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

return this;
