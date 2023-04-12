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

void sendMarkdownTestSummaryNotification(String jobId, String subject, List recipients, String additionalInfo = '', String buildUrl = "${BUILD_URL}") {
    emailext subject: (jobId ? "${subject} - ${jobId}" : subject),
            to: recipients.join(','),
            body: util.getMarkdownTestSummary('', additionalInfo, buildUrl)
}

void sendMarkdownTestSummaryNotification(String subject, List recipients, String additionalInfo = '', String buildUrl = "${BUILD_URL}") {
    sendMarkdownTestSummaryNotification('', subject, recipients, additionalInfo, buildUrl)
}
