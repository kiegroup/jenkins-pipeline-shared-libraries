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
 * @param isProjectTriggeringJobValue if it's the project triggering the job in order not to checkout it again
 */
def checkoutProject(String name, String group, Boolean isProjectTriggeringJobValue = false) {
    def changeAuthor = env.CHANGE_AUTHOR ?: ghprbPullAuthorLogin
    def changeBranch = env.CHANGE_BRANCH ?: ghprbSourceBranch
    def changeTarget = env.CHANGE_TARGET ?: ghprbTargetBranch

    configFileProvider([configFile(fileId: 'project-branches-mapping', variable: 'PROPERTIES_FILE')]) {
        def projectBranchesMapping = readProperties file: PROPERTIES_FILE
        def possibleTargetBranch = projectBranchesMapping."${name}.${changeTarget}" ?: projectBranchesMapping."${getProjectTriggeringJob()[1]}.${changeTarget}" ?: changeTarget
        if(!isProjectTriggeringJobValue && possibleTargetBranch) {
            println "Mapping ${name}:${changeTarget} to ${name}:${possibleTargetBranch}"
            changeTarget = possibleTargetBranch
        }
    }

    println "Checking out author [${changeAuthor}] branch [${changeBranch}] target [${changeTarget}]"
    if(isProjectTriggeringJobValue) {
        def sourceAuthor = env.ghprbAuthorRepoGitUrl ? getGroup(env.ghprbAuthorRepoGitUrl) : CHANGE_FORK
        githubscm.mergeSourceIntoTarget(name, "$sourceAuthor", "$changeBranch", group, "$changeTarget")
    } else {
        githubscm.checkoutIfExists(name, "$changeAuthor", "$changeBranch", group, "$changeTarget", true)
    }
    storeGitInformation("${group}/${name}")
}

/**
 *
 * @param projectUrl the github project url
 */
def getProject(String projectUrl) {
    return (projectUrl =~ /((git|ssh|http(s)?)|(git@[\w\.]+))(:(\/\/)?(github.com\/))([\w\.@\:\/\-~]+)(\.git)(\/)?/)[0][8]
}

/**
*
* @param projectUrl the github project url
*/
def getGroup(String projectUrl) {
    return getProjectGroupName(getProject(projectUrl))[0]
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
    if(isProjectTriggeringJob(projectGroupName) == true) {
        maven.runMavenWithSettings(settingsXmlId, goals, skipTests != null ? skipTests : new Properties(), "${group}_${name}.maven.log")
    } else {
        dir("${env.WORKSPACE}/${group}_${name}") {
            maven.runMavenWithSettings(settingsXmlId, goals, skipTests != null ? skipTests : new Properties(), "${group}_${name}.maven.log")
        }
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

/**
 * Returns if the project is the one triggering the job
 *
 * @param project group name array
 * @return true/false or null in case the ghprbGhRepository variable is not available
 */
def isProjectTriggeringJob(def projectGroupName) {
    if (env.ghprbGhRepository) {
        def ghprbGhRepositoryGroupName = getProjectTriggeringJob()
        def result = projectGroupName[1] == ghprbGhRepositoryGroupName[1]
        println "[INFO] is project [${projectGroupName[1]}] triggering the job? [${result}]. Project Triggering the job [${ghprbGhRepositoryGroupName[1]}]"
        return result
    } else {
        return null
    }
}

/**
 * Gets project which is triggering the job
 * @return an array where 0 is group and 1 is name
 */
def getProjectTriggeringJob() {
    return env.ghprbGhRepository ? getProjectGroupName(env.ghprbGhRepository) : null
}

/**
 *
 * Stores git information into an env variable to be retrievable at any point of the pipeline
 *
 * @param projectName to store commit
 */
def storeGitInformation(String projectName) {
    def gitInformationReport = env.GIT_INFORMATION_REPORT ? "${env.GIT_INFORMATION_REPORT}; " : ""
    gitInformationReport += "${projectName}=${githubscm.getCommit().replace(';', '').replace('=', '')} Branch [${githubscm.getBranch().replace(';', '').replace('=', '')}] Remote [${githubscm.getRemoteInfo('origin', 'url').replace(';', '').replace('=', '')}]"
    env.GIT_INFORMATION_REPORT = gitInformationReport
}

/**
 *
 * prints GIT_INFORMATION_REPORT variable
 */
 def printGitInformationReport() {
    if(env.GIT_INFORMATION_REPORT?.trim()) {
        def result = env.GIT_INFORMATION_REPORT.split(';').inject([:]) { map, token ->
            token.split('=').with { key, value ->
                map[key.trim()] = value.trim()
            }
            map
        }
        def report = '''
------------------------------------------
GIT INFORMATION REPORT
------------------------------------------
'''
        result.each{ key, value ->
            report += "${key}: ${value}\n"
        }
        println report
    } else {
        println '[WARNING] The variable GIT_INFORMATION_REPORT does not exist'
    }
}