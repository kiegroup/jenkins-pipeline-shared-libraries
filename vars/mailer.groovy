def sendEmailFailure() {
    def branch = $BRANCH_NAME ?: $ghprbSourceBranch
    emailext (
            subject: "Build $branch failed",
            body: "Build $branch failed! For more information see $BUILD_URL",
            recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']])
}