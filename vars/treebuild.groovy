import java.nio.file.Files
import java.nio.file.Paths

/**
 * Builds the downstream
 * @param filePath the path to a file with a collection of items following the pattern PROJECT_GROUP/PROJECT_NAME, for example kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param goals maven goals
 * @param skipTests boolean to skip tests or not
 */
def downstreamBuild(String filePath, String settingsXmlId, String goals, boolean skipTests) {
    def projectCollection = Files.readAllLines(Paths.get(filePath))
    def lastLine = projectCollection.get(projectCollection.size() - 1)

    println "Downstream building ${lastLine} project"
    upstreamBuild(filePath, lastLine, settingsXmlId, goals, skipTests)
}

/**
 * Builds the upstream for an specific project
 * @param filePath the path to a file with a collection of items following the pattern PROJECT_GROUP/PROJECT_NAME, for example kiegroup/drools
 * @param currentProject the project to build the stream from, like kiegroup/drools
 * @param settingsXmlId maven settings xml file id
 * @param goals maven goals
 * @param skipTests boolean to skip tests or not
 */
def upstreamBuild(String filePath, String currentProject, String settingsXmlId, String goals, boolean skipTests) {
    println "Upstream building ${currentProject} project"

    def projectCollection = Files.readAllLines(Paths.get(filePath))
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
