def sendEmailFailure() {
    emailext (
            subject: "Build $BRANCH_NAME failed",
            body: "Build $BRANCH_NAME failed! For more information see $BUILD_URL",
            recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']])
}