/**
 * Builds the downstream
 * @param the file with a collection of items following the pattern PROJECT_GROUP/PROJECT_NAME, for example kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param goals maven goals
 * @param skipTests Boolean to skip tests or not
 */
def downstreamBuild(List<String> projectCollection, String currentProject, String settingsXmlId, String propertiesFileId) {
    println "Full downstream build of project [${currentProject}] for project collection ${projectCollection}"
    checkoutProjects(projectCollection)

    def currentProjectIndex = projectCollection.findIndexOf { it == currentProject }
    // Build project tree from currentProject node
    for (i = 0; i < projectCollection.size(); i++) {
        def type = i < currentProjectIndex ? 'upstream' :  i == currentProjectIndex ? 'current' : 'downstream'
        println "Build of project [${projectCollection.get(i)}] with type [${type}]"
        buildProject(projectCollection.get(i), settingsXmlId, getGoals(projectCollection.get(i), propertiesFileId, type))
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
    println "Upstream building ${currentProject} project for ${projectCollection}"

    checkoutProjects(projectCollection, currentProject)

    // Build project tree from currentProject node
    for (i = 0; i == 0 || currentProject != projectCollection.get(i - 1); i++) {
        buildProject(projectCollection.get(i), settingsXmlId, goals, skipTests)
    }
}

/**
 * Builds the upstream for a specific project + the project itself (normal PR) + a SONARCLOUD analisys if needed
 * @param the file with a collection of items following the pattern PROJECT_GROUP/PROJECT_NAME, for example kiegroup/drools
 * @param currentProject the project to build the stream from, like kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param propertiesFileId file that defines the maven goals for each rep
 */
def pullRequestBuild(List<String> projectCollection, String currentProject, String settingsXmlId, String propertiesFileId, String sonarCloudId, sonarCloudReps = []) {
    println "Building of project ${currentProject}"
    println "Project collection: ${projectCollection}"

    checkoutProjects(projectCollection, currentProject)

    // Build project tree from currentProject node
    for (i = 0; currentProject != projectCollection.get(i); i++) {
        println "Current Upstream Project:" + projectCollection.get(i)
        buildProject(projectCollection.get(i), settingsXmlId, getGoals(projectCollection.get(i), propertiesFileId, 'upstream'))
    }

    println "Build of current project: ${currentProject}"
    buildProject(currentProject, settingsXmlId, getGoals(currentProject, propertiesFileId))

    if(sonarCloudReps.contains(currentProject)) {
        println "SONARCLOUD analysis of : ${currentProject}"
        buildProjectSonar(currentProject, settingsXmlId, getGoals(currentProject, propertiesFileId, 'sonarcloud'), sonarCloudId)
    } else {
        println "INFO: ${currentProject} project is not for SONARCLOUD analysis"
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
 * Fetches the artifacts from a Jenkins properties file
 * @param project - current project
 * @param propertiesFileId - pointing to a Jenkins properties file
 */
def getArtifacts(String project, String propertiesFileId) {
    configFileProvider([configFile(fileId: propertiesFileId, variable: 'PROPERTIES_FILE')]) {
        def propertiesFile = readProperties file: PROPERTIES_FILE
        return propertiesFile."artifacts.${project}" ?: propertiesFile."artifacts.default"
    }
}

/**
 * Builds the compile downstream (upstream projects, the current project and the downstream projects)
 * @param the file with a collection of items following the pattern PROJECT_GROUP/PROJECT_NAME, for example kiegroup/drools
 * @param currentProject the project to build the stream from, like kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param propertiesFileId file that defines the maven goals for each rep
 */
def pullCompileDownstreamBuild(List<String> projectCollection, String currentProject, String settingsXmlId, String propertiesFileId) {
    println "Compile downstream build of project [${currentProject}] for project collection ${projectCollection}"
    checkoutProjects(projectCollection)

    def currentProjectIndex = projectCollection.findIndexOf { it == currentProject }
    // Build project tree from currentProject node
    for (i = 0; i < projectCollection.size(); i++) {
        def type = i < currentProjectIndex ? 'upstream' :  i == currentProjectIndex ? 'current' : 'downstream'
        println "Build of project [${projectCollection.get(i)}] with type [${type}]"
        buildProject(projectCollection.get(i), settingsXmlId, getGoals(projectCollection.get(i), propertiesFileId, type))
    }
}

/**
 *
 * @param project a string following the pattern PROJECT_GROUP/PROJECT_NAME, for example kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param goals maven goals
 * @param sonarCloudId token for sonarcloud
 */
def buildProjectSonar(String project, String settingsXmlId, String goals, String sonarCloudId) {
    def projectGroupName = getProjectGroupName(project)
    def group = projectGroupName[0]
    def name = projectGroupName[1]

    println "Building ${group}/${name}"
    dir("${env.WORKSPACE}/${group}_${name}") {
        maven.runMavenWithSettingsSonar(settingsXmlId, goals, sonarCloudId, "${group}_${name}.maven.log")
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
    dir("${env.WORKSPACE}/${group}_${name}") {
        maven.runMavenWithSettings(settingsXmlId, goals, skipTests != null ? skipTests : new Properties(), "${group}_${name}.maven.log")
    }
}

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
    def changeGroupTarget = env.ghprbGhRepository ? getProjectGroupName(env.ghprbGhRepository)[0] : group
    println "Checking out author [${changeAuthor}] branch [${changeBranch}] target [${changeGroupTarget}/${changeTarget}]"
    githubscm.checkoutIfExists(name, "$changeAuthor", "$changeBranch", "$changeGroupTarget", "$changeTarget")
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
