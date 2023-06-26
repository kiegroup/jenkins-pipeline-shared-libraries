/**
 * Checks out the project collection
 *
 * @param projectCollection the list of projects to be checked out
 * @param limitProject the project to stop
 */
def checkoutProjects(List<String> projectCollection, String limitProject = null) {
    println "Checking out projects ${projectCollection}"

    for (i = 0; limitProject ? (i == 0 || limitProject != projectCollection.get(i - 1)) : i < projectCollection.size(); i++) {
        def projectGroupName = getProjectGroupName(projectCollection.get(i))
        def group = projectGroupName[0]
        def name = projectGroupName[1]
        if (isProjectTriggeringJob(projectGroupName)) {
            checkoutProject(name, group)
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
def checkoutProject(String name, String group) {
    println "Checking out ${group}/${name}"
    def changeAuthor = env.CHANGE_AUTHOR ?: ghprbPullAuthorLogin
    def changeBranch = env.CHANGE_BRANCH ?: ghprbSourceBranch
    def changeTarget = env.CHANGE_TARGET ?: ghprbTargetBranch

    configFileProvider([configFile(fileId: 'project-branches-mapping', variable: 'PROPERTIES_FILE')]) {
        def mapBranch = getMapToBranch(PROPERTIES_FILE, name, changeTarget)
        if (mapBranch) {
            println "Mapping ${name}:${changeTarget} to ${name}:${mapBranch}"
            changeTarget = mapBranch
        }
    }

    def sourceAuthor = env.ghprbAuthorRepoGitUrl ? getGroup(env.ghprbAuthorRepoGitUrl) : env.CHANGE_FORK ?: getGroup(env.GIT_URL)
    println "Checking out ${group}/${name} source author [${sourceAuthor}] branch [${changeBranch}] target [${changeTarget}]"
    if (isProjectTriggeringJob(getProjectGroupName(name, group))) {
        def sourceRepositoryName = githubscm.getForkedProjectName(group, name, sourceAuthor) ?: name
        githubscm.mergeSourceIntoTarget(sourceRepositoryName, "$sourceAuthor", "$changeBranch", name, group, "$changeTarget")
    } else {
        githubscm.checkoutIfExists(name, "$sourceAuthor", "$changeBranch", group, "$changeTarget", true)
    }
    storeGitInformation("${group}/${name}")
}

def getMapToBranch(def propertiesFile, String projectName, String changeTarget) {
    def isCurrentProjectTriggeringJob = isProjectTriggeringJob(getProjectGroupName(projectName))
    if (!isCurrentProjectTriggeringJob) {
        def projectBranchesMapping = readProperties file: propertiesFile
        def projectTriggeringJob = getProjectTriggeringJob()
        def triggeringJobProjectMap = projectBranchesMapping."${projectTriggeringJob[1]}.trigger.${changeTarget}"
        def currentProjectMap = projectBranchesMapping."${projectName}.${changeTarget}"

        return currentProjectMap ?: triggeringJobProjectMap
    } else {
        return null
    }
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
    if (isProjectTriggeringJob(projectGroupName) == true) {
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
 * @param propertiesFilePath - path to file of properties used
 * @param type (can be "current" or "upstream")
 */
def getGoals(String project, String propertiesFilePath, String type = 'current') {
    def propertiesFile = readProperties file: propertiesFilePath
    if (!propertiesFile) {
        println "[WARNING] File ${propertiesFilePath} does not exist, trying on env folder."
        propertiesFile = readProperties file: propertiesFilePath.replaceAll(env.WORKSPACE, "${env.WORKSPACE}/.ci-env")
    }
    assert propertiesFile: "Properties file ${propertiesFilePath} does not exist."
    return propertiesFile."goals.${project}.${type}" ?: propertiesFile."goals.default.${type}"
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
    if (!env.ghprbGhRepository && !env.GIT_URL) {
        throw new Exception("There's no way to get which project is triggering the job");
    }
    return getProjectGroupName(env.ghprbGhRepository ?: getProject(env.GIT_URL));
}

/**
 * Returns the path to the project dir
 * @param projectGroupName
 * @return
 */
def getProjectDirPath(String project, String defaultGroup = "kiegroup") {
    def projectGroupName = getProjectGroupName(project, defaultGroup)
    return "${env.WORKSPACE}/${projectGroupName[0]}_${projectGroupName[1]}"
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

    def gitHashes = env.GIT_INFORMATION_HASHES ? "${env.GIT_INFORMATION_HASHES};" : ""
    gitHashes += "${projectName}=${githubscm.getCommitHash()}"
    env.GIT_INFORMATION_HASHES = gitHashes
}

/**
 *
 * prints GIT_INFORMATION_REPORT variable
 */
def printGitInformationReport() {
    if (env.GIT_INFORMATION_REPORT?.trim()) {
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
        result.each { key, value ->
            report += "${key}: ${value}\n"
        }
        println report
    } else {
        println '[WARNING] The variable GIT_INFORMATION_REPORT does not exist'
    }
}

/*
 * Get the next major/minor/micro version, with a specific suffix if needed.
 * The version string needs to be in the form X.Y.Z
*/

def getNextVersion(String version, String type, String suffix = 'SNAPSHOT', boolean resetSubVersions = true) {
    assert ['major', 'minor', 'micro'].contains(type)
    Integer[] versionSplit = parseVersion(version)
    if (versionSplit != null) {
        int majorVersion = versionSplit[0] + (type == 'major' ? 1 : 0)
        int minorVersion = resetSubVersions && type == 'major' ? 0 : (versionSplit[1] + (type == 'minor' ? 1 : 0))
        int microVersion = resetSubVersions && (type == 'major' || type == 'minor') ? 0 : (versionSplit[2] + (type == 'micro' ? 1 : 0))
        return "${majorVersion}.${minorVersion}.${microVersion}${suffix ? '-' + suffix : ''}"
    } else {
        return null
    }
}

/*
 * It parses a version string, which needs to be in the format X.Y.Z or X.Y.Z.suffix and returns the 3 numbers
 * in an array. The optional suffix must not be numeric.
 * <p>
 * Valid version examples:
 * 1.0.0
 * 1.0.0.Final
*/

Integer[] parseVersion(String version) {
    String[] versionSplit = version.split("\\.")
    boolean hasNonNumericSuffix = versionSplit.length == 4 && !(versionSplit[3].isNumber())
    if (versionSplit.length == 3 || hasNonNumericSuffix) {
        if (versionSplit[0].isNumber() && versionSplit[1].isNumber() && versionSplit[2].isNumber()) {
            Integer[] vs = new Integer[3]
            vs[0] = Integer.parseInt(versionSplit[0])
            vs[1] = Integer.parseInt(versionSplit[1])
            vs[2] = Integer.parseInt(versionSplit[2])
            return vs
        } else {
            error "Version ${version} is not in the required format. The major, minor, and micro parts should contain only numeric characters."
        }
    } else {
        error "Version ${version} is not in the required format X.Y.Z or X.Y.Z.suffix."
    }
}

String getReleaseBranchFromVersion(String version) {
    Integer[] versionSplit = parseVersion(version)
    return "${versionSplit[0]}.${versionSplit[1]}.x"
}

String calculateTargetReleaseBranch(String currentReleaseBranch, int addToMajor = 0, int addToMinor = 0) {
    String targetBranch = currentReleaseBranch
    String [] versionSplit = targetBranch.split("\\.")
    if (versionSplit.length == 3
        && versionSplit[0].isNumber()
        && versionSplit[1].isNumber()
        && versionSplit[2] == 'x') {
        Integer newMajor = Integer.parseInt(versionSplit[0]) + addToMajor
        Integer newMinor = Integer.parseInt(versionSplit[1]) + addToMinor
        targetBranch = "${newMajor}.${newMinor}.x"
    } else {
        println "Cannot parse targetBranch as release branch so going further with current value: ${targetBranch}"
    }
    return targetBranch
}

/**
 * It prepares the environment to avoid problems with plugins. For example files from SCM pipeline are deleted during checkout
 */
def prepareEnvironment() {
    println """
[INFO] Preparing Environment
[INFO] Copying WORKSPACE content env folder
    """
    def envFolderName = '.ci-env'
    if (fileExists("${env.WORKSPACE}/${envFolderName}")) {
        println "[WARNING] folder ${env.WORKSPACE}/${envFolderName} already exist, won't create env folder again."
    } else {
        dir(env.WORKSPACE) {
            sh "mkdir ${envFolderName}"
            sh "cp -r `ls -A | grep -v '${envFolderName}'` ${envFolderName}/"
        }
    }
}

/*
* Generate a hash composed of alphanumeric characters (lowercase) of a given size
*/

String generateHash(int size) {
    String alphabet = (('a'..'z') + ('0'..'9')).join("")
    def random = new Random()
    return (1..size).collect { alphabet[random.nextInt(alphabet.length())] }.join("")
}

String generateTempFile() {
    return sh(returnStdout: true, script: 'mktemp').trim()
}

String generateTempFolder() {
    return sh(returnStdout: true, script: 'mktemp -d').trim()
}

void executeWithCredentialsMap(Map credentials, Closure closure) {
    if (credentials.token) {
        withCredentials([string(credentialsId: credentials.token, variable: 'QUAY_TOKEN')]) {
            closure()
        }
    } else if (credentials.usernamePassword) {
        withCredentials([usernamePassword(credentialsId: credentials.usernamePassword, usernameVariable: 'QUAY_USER', passwordVariable: 'QUAY_TOKEN')]) {
            closure()
        }
    } else {
        error 'No credentials given to execute the given closure'
    }
}

void cleanNode(String containerEngine = '') {
    println '[INFO] Clean workspace'
    cleanWs()
    println '[INFO] Workspace cleaned'
    println '[INFO] Cleanup Maven artifacts'
    maven.cleanRepository()
    println '[INFO] .m2/repository cleaned'
    if (containerEngine) {
        println "[INFO] Cleanup ${containerEngine} containers/images"
        cloud.cleanContainersAndImages(containerEngine)
    }
}

def spaceLeft() {
    dir(env.WORKSPACE) {
        println '[INFO] space left on the machine'
        sh 'df -h'
        println '[INFO] space of /home/jenkins'
        sh "du -h -d1 /home/jenkins"
        println '[INFO] space of workspace'
        sh "du -h -d3 /home/jenkins/workspace"
    }
}

def replaceInAllFilesRecursive(String findPattern, String oldValueSedPattern, String newSedValue) {
    sh "find . -name '${findPattern}' -type f -exec sed -i 's/${oldValueSedPattern}/${newSedValue}/g' {} \\;"
}

/*
* Removes any partial downloaded dependencies from .m2 if the previous run was interrupted and no post actions were
* executed (cleanRepository()) and a new build is executed on the same machine
*/
def rmPartialDeps(){
    dir("${env.WORKSPACE}/.m2") {
        sh "find . -regex \".*\\.part\\(\\.lock\\)?\" -exec rm -rf {} \\;"
    }
}

String retrieveConsoleLog(int numberOfLines = 100, String buildUrl = "${BUILD_URL}") {
    return sh(returnStdout: true, script: "wget --no-check-certificate -qO - ${buildUrl}consoleText | tail -n ${numberOfLines}")
}

String archiveConsoleLog(String id = '', int numberOfLines = 100, String buildUrl = "${BUILD_URL}") {
    String filename = "${id ? "${id}_" : ''}console.log"
    sh "rm -rf ${filename}"
    writeFile(text: retrieveConsoleLog(numberOfLines, buildUrl), file: filename)
    archiveArtifacts(artifacts: filename)
}

def retrieveTestResults(String buildUrl = "${BUILD_URL}") {
    return readJSON(text: sh(returnStdout: true, script: "wget --no-check-certificate -qO - ${buildUrl}testReport/api/json"))
}

def retrieveFailedTests(String buildUrl = "${BUILD_URL}") {
    def testResults = retrieveTestResults(buildUrl)

    def allCases = []
    testResults.suites?.each { testSuite ->
        allCases.addAll(testSuite.cases)
    }

    def failedTests = []
    testResults.suites?.each { testSuite ->
        testSuite.cases?.each { testCase ->
            if (!['PASSED', 'SKIPPED', 'FIXED'].contains(testCase.status)) {
                def failedTest = [:]
                
                boolean hasSameNameCases = allCases.findAll { it.name == testCase.name && it.className == testCase.className }.size() > 1

                failedTest.status = testCase.status

                // Retrieve class name
                fullClassName = testCase.className
                int lastIndexOf = fullClassName.lastIndexOf('.')
                packageName = fullClassName.substring(0, lastIndexOf)
                className = fullClassName.substring(lastIndexOf + 1)

                failedTest.name = testCase.name
                failedTest.packageName = packageName
                failedTest.className = className
                failedTest.enclosingBlockNames = testSuite.enclosingBlockNames?.reverse()?.join(' / ')

                failedTest.fullName = "${packageName}.${className}.${failedTest.name}"
                // If other cases have the same className / name, Jenkins uses the enclosingBlockNames for the URL distinction
                if (hasSameNameCases && testSuite.enclosingBlockNames) {
                    failedTest.fullName = "${testSuite.enclosingBlockNames.reverse().join(' / ')} / ${failedTest.fullName}"
                }

                // Construct test url
                String urlLeaf = ''
                // If other cases have the same className / name, Jenkins uses the enclosingBlockNames for the URL distinction
                if (hasSameNameCases && testSuite.enclosingBlockNames) {
                    urlLeaf += testSuite.enclosingBlockNames.reverse().join('___')
                }
                urlLeaf += urlLeaf ? '___' : urlLeaf
                urlLeaf += "${failedTest.name == "(?)" ? "___" : failedTest.name}/"
                urlLeaf = urlLeaf.replaceAll(' ', '_')
                                    .replaceAll('&', '_')
                                    .replaceAll('-', '_')
                failedTest.url = "${buildUrl}testReport/${packageName}/${className}/${urlLeaf}"

                failedTest.details = [null, 'null'].contains(testCase.errorDetails) ? '' : testCase.errorDetails
                failedTest.stacktrace = [null, 'null'].contains(testCase.errorStackTrace) ? '' : testCase.errorStackTrace
                failedTests.add(failedTest)
            }
        }
    }

    return failedTests
}

String retrieveArtifact(String artifactPath, String buildUrl = "${BUILD_URL}") {
    String finalUrl = "${buildUrl}artifact/${artifactPath}"
    String httpCode = sh(returnStdout: true, script: "curl -o /dev/null --silent -Iw '%{http_code}' ${finalUrl}")
    return httpCode == "200" ? sh(returnStdout: true, script: "wget --no-check-certificate -qO - ${finalUrl}") : '' 
}

def retrieveJobInformation(String buildUrl = "${BUILD_URL}") {
    return readJSON(text: sh(returnStdout: true, script: "wget --no-check-certificate -qO - ${buildUrl}api/json"))
}

boolean isJobResultSuccess(String jobResult) {
    return jobResult == 'SUCCESS'
}

boolean isJobResultFailure(String jobResult) {
    return jobResult == 'FAILURE'
}

boolean isJobResultAborted(String jobResult) {
    return jobResult == 'ABORTED'
}

boolean isJobResultUnstable(String jobResult) {
    return jobResult == 'UNSTABLE'
}

/*
* Return the build/test summary of a job
*
* outputStyle possibilities: 'ZULIP' (default), 'GITHUB'
*/
String getMarkdownTestSummary(String jobId = '', String additionalInfo = '', String buildUrl = "${BUILD_URL}", String outputStyle = 'ZULIP') {
    def jobInfo = retrieveJobInformation(buildUrl)

    // Check if any *_console.log is available as artifact first
    String defaultConsoleLogId = 'Console Logs'
    Map consoleLogs = jobInfo.artifacts?.collect { it.fileName }
            .findAll { it.endsWith('console.log') }
            .collectEntries { filename ->
                int index = filename.lastIndexOf('_')
                String logId = index > 0 ? filename.substring(0, index) : defaultConsoleLogId
                return [ (logId) : retrieveArtifact(filename, buildUrl) ]
            } ?: [ (defaultConsoleLogId) : retrieveConsoleLog(50, buildUrl)]

    String jobResult = jobInfo.result
    String summary = """
${jobId ? "**${jobId} job**" : 'Job'} ${formatBuildNumber(outputStyle, BUILD_NUMBER)} was: **${jobResult}**
"""

    if (!isJobResultSuccess(jobResult)) {
        summary += "Possible explanation: ${getResultExplanationMessage(jobResult)}\n"
    }

    if (additionalInfo) {
        summary += """
${additionalInfo}
"""    
    }

    if (!isJobResultSuccess(jobResult)) {
        boolean testResultsFound = false
        summary += "\nPlease look here: ${buildUrl}display/redirect"

        try {
            def testResults = retrieveTestResults(buildUrl)
            def failedTests = retrieveFailedTests(buildUrl)
            testResultsFound=true

            summary += """
\n**Test results:**
- PASSED: ${testResults.passCount}
- FAILED: ${testResults.failCount}
"""

            summary += 'GITHUB'.equalsIgnoreCase(outputStyle) ? """
Those are the test failures: ${failedTests.size() <= 0 ? 'none' : '\n'}${failedTests.collect { failedTest ->
                return """<details>
<summary><a href="${failedTest.url}">${failedTest.fullName}</a></summary>
${formatTextForHtmlDisplay(failedTest.details ?: failedTest.stacktrace)}
</details>"""
}.join('\n')}
"""   
                :  """
Those are the test failures: ${failedTests.size() <= 0 ? 'none' : '\n'}${failedTests.collect { failedTest ->
                return """```spoiler [${failedTest.fullName}](${failedTest.url})
${failedTest.details ?: failedTest.stacktrace}
```"""
}.join('\n')}
"""
        } catch (err) {
            echo 'No test results found'
        }

        // Display console logs if no test results found
        if (!(jobResult == 'UNSTABLE' && testResultsFound)) {
            summary += 'GITHUB'.equalsIgnoreCase(outputStyle) ? """
See console log:
${consoleLogs.collect { key, value ->
return """<details>
<summary><b>${key}</b></summary>
${formatTextForHtmlDisplay(value)}
</details>
"""
}.join('')}"""
                :  """
See console log:
${consoleLogs.collect { key, value ->
return """```spoiler ${key}
${value}
```
"""
}.join('')}"""
        }
    }

    return summary
}

String getResultExplanationMessage(String jobResult) {
    switch (jobResult) {
        case 'SUCCESS':
            return 'Do I need to explain ?'
        case 'UNSTABLE':
            return 'This should be test failures'
        case 'FAILURE':
            return 'Pipeline failure or project build failure'
        case 'ABORTED':
            return 'Most probably a timeout, please review'
        default:
            return 'Woops ... I don\'t know about this result value ... Please ask maintainer.'
    }
}

String formatTextForHtmlDisplay(String text) {
    return text.replaceAll('\n', '<br/>')
}

String formatBuildNumber(String outputStyle, String buildNumber) {
    return 'GITHUB'.equalsIgnoreCase(outputStyle) ? "`#${buildNumber}`" : "#${buildNumber}"
}

/**
 * Encode the provided string value in the provided encoding
 * @param value string to encode
 * @param encoding [default UTF-8]
 * @return the encoded string
 */
String encode(String value, String encoding='UTF-8') {
    return URLEncoder.encode(value, encoding)
}

/**
 * Serialize the parameters converting a Map into an URL query string, like:
 * {A: 1, B: 2} --> 'A=1&B=2'
 * @param params key-value map representation of the parameters
 * @return URL query string
 */
String serializeQueryParams(Map params) {
    return params.collect { "${it.getKey()}=${encode(it.getValue() as String)}" }.join('&')
}

/**
 * Execute the provided closure within Kerberos authentication context
 * @param keytabId id of the keytab to be used
 * @param closure code to run in the kerberos auth context 
 * @param domain kerberos domain to look for into the keytab
 * @param retry number of max retries to perform if kinit fails
 */
def withKerberos(String keytabId, Closure closure, String domain = 'REDHAT.COM', int nRetries = 5) {
    withCredentials([file(credentialsId: keytabId, variable: 'KEYTAB_FILE')]) {
        env.KERBEROS_PRINCIPAL = sh(returnStdout: true, script: "klist -kt $KEYTAB_FILE |grep $domain | awk -F' ' 'NR==1{print \$4}' ").trim()

        if (!env.KERBEROS_PRINCIPAL?.trim()) {
            throw new Exception("[ERROR] found blank KERBEROS_PRINCIPAL, kerberos authetication failed.")
        }

        // check if kerberos authentication already exists with provided principal
        def currentPrincipal = sh(returnStdout: true, script: "klist | grep -i 'Default principal' | awk -F':' 'NR==1{print \$2}' ").trim()

        if (currentPrincipal != env.KERBEROS_PRINCIPAL) {
            def kerberosStatus = 0
            for (int i = 0; i < nRetries; i++) {
                kerberosStatus = sh(returnStatus: true, script: "kinit ${env.KERBEROS_PRINCIPAL} -kt $KEYTAB_FILE")
                if (kerberosStatus == 0) {
                    // exit at first success
                    break
                }
            }

            // if the kerberos status is still != 0 after nRetries throw exception
            if (kerberosStatus != 0) {
                throw new Exception("[ERROR] kinit failed with non-zero status.")
            }
        } else {
            println "[INFO] ${env.KERBEROS_PRINCIPAL} already authenticated, skipping kinit."
        }

        closure()
    }
}

def runWithPythonVirtualEnv(String cmd, String virtualEnvName, boolean returnStdout = false) {
    return sh(returnStdout: returnStdout, script: """
source ~/virtenvs/${virtualEnvName}/bin/activate
${cmd}
""")
}

String displayDurationFromSeconds(int durationInSec) {
    String result = ''
    int seconds = durationInSec
    int minutes = durationInSec / 60
    if (minutes > 0) {
        seconds = seconds - minutes * 60
        int hours = minutes / 60
        if (hours > 0) {
            minutes = minutes - hours*60
            result += "${hours}h"
        }
        result += "${minutes}m"
    }
    result += "${seconds}s"
}