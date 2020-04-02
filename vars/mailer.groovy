def sendEmailFailure() {
    def branch = env.CHANGE_BRANCH ?: env.ghprbSourceBranch
    emailext (
            subject: "Build $branch failed",
            body: "Build $branch failed! For more information see $BUILD_URL",
            recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']])
}
