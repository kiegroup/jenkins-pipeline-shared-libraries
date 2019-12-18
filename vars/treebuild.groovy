/**
 * Builds the downstream
 * @param projectCollection a collection of items following the pattern PROJECT_GROUP/PROJECT_NAME, for example kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param goals maven goals
 * @param skipTests boolean to skip tests or not
 */
def downstreamBuild(Collection<String> projectCollection, String settingsXmlId, String goals, boolean skipTests) {
    def lastLine = projectCollection.get(projectCollection.size() - 1)

    println "Downstream building ${lastLine} project"
    upstreamBuild(projectCollection, lastLine, settingsXmlId, goals, skipTests)
}

/**
 * Builds the upstream for an specific project
 * @param projectCollection a collection of items following the pattern PROJECT_GROUP/PROJECT_NAME, for example kiegroup/drools
 * @param currentProject the project to build the stream from, like kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param goals maven goals
 * @param skipTests boolean to skip tests or not
 */
def upstreamBuild(Collection<String> projectCollection, String currentProject, String settingsXmlId, String goals, boolean skipTests) {
    println "Upstream building ${currentProject} project"

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
 * @param skipTests boolean to skip tests or not
 */
def buildProject(String project, String settingsXmlId, String goals, boolean skipTests) {
    def projectGroup = project.split("\\/")[0]
    def projectName = project.split("\\/")[1]
    println "Building ${projectGroup}/${projectName}"
    sh "mkdir -p ${projectGroup}_${projectName}"
    sh "cd ${projectGroup}_${projectName}"
    githubscm.checkoutIfExists(projectName, "$CHANGE_AUTHOR", "$CHANGE_BRANCH", projectGroup, "$CHANGE_TARGET")
    maven.runMavenWithSettings(settingsXmlId, goals, skipTests)
    sh "cd .."
}

/**
 *
 * @param projectUrl the github project url
 */
def getProject(String projectUrl) {
    return (projectUrl =~ /((git|ssh|http(s)?)|(git@[\w\.]+))(:(\/\/)?(github.com\\/))([\w\.@\:\/\-~]+)(\.git)(\/)?/)[0][8]
}

return this;
