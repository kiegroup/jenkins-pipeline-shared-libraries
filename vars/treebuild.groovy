/**
 * Builds the downstream
 * @param the file with a collection of items following the pattern PROJECT_GROUP/PROJECT_NAME, for example kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param goals maven goals
 * @param skipTests Boolean to skip tests or not
 */
def downstreamBuild(List<String> projectCollection, String settingsXmlId, String goals, Boolean skipTests = null) {
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

    checkoutProjects(projectCollection, currentProject)

    // Build project tree from currentProject node
    for (i = 0; i == 0 || currentProject != projectCollection.get(i-1); i++) {
        buildProject(projectCollection.get(i), settingsXmlId, goals, skipTests)
    }
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
    if(isProjectTriggeringJob(projectGroupName) == true) {
        maven.runMavenWithSettings(settingsXmlId, goals, skipTests != null ? skipTests : new Properties(), "${group}_${name}.maven.log")
    } else {
        dir("${env.WORKSPACE}/${group}_${name}") {
            maven.runMavenWithSettings(settingsXmlId, goals, skipTests != null ? skipTests : new Properties(), "${group}_${name}.maven.log")
        }
    }
}

/**
 * Checks out the project collection
 *
 * @param projectCollection the list of projects to be checked out
 * @param limitProject the project to stop
 */
def checkoutProjects(List<String> projectCollection, String limitProject) {
    println "Checking out projects ${projectCollection}"

    for (i = 0; i == 0 || limitProject != projectCollection.get(i-1); i++) {
        def projectGroupName = getProjectGroupName(projectCollection.get(i))
        def group = projectGroupName[0]
        def name = projectGroupName[1]
        if(isProjectTriggeringJob(projectGroupName) == true) {
            checkoutProject(name, group, true)
        } else {
            sh "mkdir -p ${group}_${name}"
            dir("${env.WORKSPACE}/${group}_${name}") {
                checkoutProject(name, group)
            }
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
def checkoutProject(String name, String group, Boolean isProjectTriggeringJobValue = false) {
    def changeAuthor = env.CHANGE_AUTHOR ?: ghprbPullAuthorLogin
    def changeBranch = env.CHANGE_BRANCH ?: ghprbSourceBranch
    def changeTarget = env.CHANGE_TARGET ?: ghprbTargetBranch
    println "Checking out author [${changeAuthor}] branch [${changeBranch}] target [${changeTarget}]"
    if(isProjectTriggeringJobValue) {
        githubscm.mergeSourceIntoTarget(name, "$changeAuthor", "$changeBranch", group, "$changeTarget")
    } else {
        githubscm.checkoutIfExists(name, "$changeAuthor", "$changeBranch", group, "$changeTarget", true)
    }
    util.storeGitInformation("${group}/${name}")
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
 * Returns is the project is the one triggering the job
 *
 * @param project group name array
 * @return true/false or null in case the ghprbGhRepository variable is not available
 */
def isProjectTriggeringJob(def projectGroupName) {
    if(env.ghprbGhRepository) {
        def ghprbGhRepositoryGroupName = getProjectGroupName(env.ghprbGhRepository)
        def result = projectGroupName[1] == ghprbGhRepositoryGroupName[1]
        println "[INFO] is project [${projectGroupName[1]}] triggering the job? [${result}]. Project Triggering the job [${ghprbGhRepositoryGroupName[1]}]"
        return result
    } else {
        return null
    }
}

return this;