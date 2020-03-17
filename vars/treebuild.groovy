/**
 * Builds the downstream
 * @param the file with a collection of items following the pattern PROJECT_GROUP/PROJECT_NAME, for example kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param goals maven goals
 * @param skipTests boolean to skip tests or not
 */
def downstreamBuild(def projectCollection, String settingsXmlId, String goals, boolean skipTests) {
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
 * @param skipTests boolean to skip tests or not
 */
def upstreamBuild(def projectCollection, String currentProject, String settingsXmlId, String goals, boolean skipTests) {
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
 * @param skipTests boolean to skip tests or not
 */
def buildProject(String project, String settingsXmlId, String goals, boolean skipTests, String defaultGroup = "kiegroup") {
    def projectNameGroup = project.split("\\/")
    def group = projectNameGroup.size() > 1 ? projectNameGroup[0] : defaultGroup
    def name = projectNameGroup.size() > 1 ? projectNameGroup[1] : project
    println "Building ${group}/${name}"
    sh "mkdir -p ${group}_${name}"
    sh "cd ${group}_${name}"
    githubscm.checkoutIfExists(name, "$CHANGE_AUTHOR", "$CHANGE_BRANCH", group, "$CHANGE_TARGET")

    maven.runMavenWithSettings(settingsXmlId, goals, skipTests)
    sh "cd .."
    sh "rm -rf ${group}_${name}"
}

/**
 *
 * @param projectUrl the github project url
 */
def getProject(String projectUrl) {
    return (projectUrl =~ /((git|ssh|http(s)?)|(git@[\w\.]+))(:(\/\/)?(github.com\\/))([\w\.@\:\/\-~]+)(\.git)(\/)?/)[0][8]
}

return this;
