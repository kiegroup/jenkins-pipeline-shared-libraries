def sendEmailFailure() {
    def branch = env.CHANGE_BRANCH ?: env.ghprbSourceBranch
    emailext (
            subject: "Build $branch failed",
            body: "Build $branch failed! For more information see $BUILD_URL",
            recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']])
}

def sendEmail_failedPR(String additionalSubject = null ) {
    emailext(
            subject: "${additionalSubject?.trim() || additionalSubject?.trim() != null ? additionalSubject?.trim() : 'PR'} #$ghprbPullId of $ghprbGhRepository: $ghprbPullTitle failed",
            body:  """
                   Pull request #$ghprbPullId of $ghprbGhRepository: $ghprbPullTitle FAILED
                   Build log: ${BUILD_URL}consoleText
                   Failed tests \${TEST_COUNTS,var=\"fail\"}: ${BUILD_URL}testReport
                   (IMPORTANT: For visiting the links you need to have access to Red Hat VPN. In case you don\'t have access to RedHat VPN please download and decompress attached file.)
                   """,
            attachmentsPattern: 'error.log.gz',
            recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']])
}

def sendEmail_unstablePR(String additionalSubject = null ) {
    emailext(
            subject: "${additionalSubject?.trim() || additionalSubject?.trim() != null ? additionalSubject?.trim() : 'PR'} #$ghprbPullId of $ghprbGhRepository: $ghprbPullTitle was unstable",
            body:  """
                   Pull request #$ghprbPullId of $ghprbGhRepository: $ghprbPullTitle was UNSTABLE
                   Build log: ${BUILD_URL}consoleText
                   Failed tests \${TEST_COUNTS,var=\"fail\"}: ${BUILD_URL}testReport
                   (IMPORTANT: For visiting the links you need to have access to Red Hat VPN)
                   ***********************************************************************************************************************************************************
                   \${FAILED_TESTS}
                   """,
            recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']])
}

def sendEmail_fixedPR(String additionalSubject = null ) {
    emailext(
            subject: "${additionalSubject?.trim() || additionalSubject?.trim() != null ? additionalSubject?.trim() : 'PR'} #$ghprbPullId of $ghprbGhRepository: $ghprbPullTitle is fixed and was SUCCESSFUL",
            body: '',
            recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']])
}

def sendEmail_abortedPR(String additionalSubject = null ) {
    emailext(
            subject: "${additionalSubject?.trim() || additionalSubject?.trim() != null ? additionalSubject?.trim() : 'PR'} #$ghprbPullId of $ghprbGhRepository: $ghprbPullTitle was ABORTED",
            body: '',
            recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']])
}

def buildLogScriptPR () {
    dir("$WORKSPACE") {
        sh 'touch trace.sh'
        sh 'chmod 755 trace.sh'
        sh 'echo "wget --no-check-certificate ${BUILD_URL}consoleText" >> trace.sh'
        sh 'echo "tail -n 750 consoleText >> error.log" >> trace.sh'
        sh 'echo "gzip error.log" >> trace.sh'
    }
}

void sendZulipTestSummaryNotification(String subject, List recipients, String buildUrl = "${BUILD_URL}") {
    // Check if console.log is available as artifact first
    String consoleLog = util.retrieveArtifact('console.log', buildUrl)
    consoleLog = consoleLog ?: util.retrieveConsoleLog(100, buildUrl)

    String jobResult = util.retrieveJobInformation(buildUrl).result
    String body = """
**Deploy job** #${BUILD_NUMBER} was: **${jobResult}**
"""

    if (!util.isJobResultSuccess(jobResult)) {
        body += "Possible explanation: ${getErrorExplanationMessage(jobResult)}\n"

        try {
            def testResults = util.retrieveTestResults(buildUrl)
            def failedTests = util.retrieveFailedTests(buildUrl)

            body += """
\n**Test results:**
- PASSED: ${testResults.passCount}
- FAILED: ${testResults.failCount}

Those are the test failures: ${failedTests.size() <= 0 ? 'none' : '\n'}${failedTests.collect { failedTest ->
                return "- [${failedTest.fullName}](${failedTest.url})"
}.join('\n')}
"""
        } catch (err) {
            echo 'No test results found'
        }

        body += """
\nPlease look here: ${buildUrl} or see console log:

```spoiler Logs
${consoleLog}
```
"""
    }

    emailext subject: subject,
            to: recipients.join(','),
            body: body
}

String getErrorExplanationMessage(String jobResult) {
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
