import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class MailerSpec extends JenkinsPipelineSpecification {
    def groovyScript = null

    def setup() {
        groovyScript = loadPipelineScriptForTest("vars/mailer.groovy")
        groovyScript.metaClass.WORKSPACE = '/'
    }

    def "[mailer.groovy] sendEmailFailure without CHANGE_BRANCH"() {
        setup:
        def env = [:]
        env['ghprbSourceBranch'] = 'PR_1'
        groovyScript.getBinding().setVariable("env", env)
        groovyScript.getBinding().setVariable("BUILD_URL", 'https://redhat.com/')
        when:
        groovyScript.sendEmailFailure()
        then:
        1 * getPipelineMock("emailext")(['subject': 'Build PR_1 failed', body: 'Build PR_1 failed! For more information see https://redhat.com/', 'recipientProviders': [['$class': 'DevelopersRecipientProvider'], ['$class': 'RequesterRecipientProvider']]])
    }

    def "[mailer.groovy] sendEmailFailure with CHANGE_BRANCH"() {
        setup:
        def env = [:]
        env['CHANGE_BRANCH'] = 'master'
        groovyScript.getBinding().setVariable("env", env)
        groovyScript.getBinding().setVariable("BUILD_URL", 'https://redhat.com/')
        when:
        groovyScript.sendEmailFailure()
        then:
        1 * getPipelineMock("emailext")(['subject': 'Build master failed', body: 'Build master failed! For more information see https://redhat.com/', 'recipientProviders': [['$class': 'DevelopersRecipientProvider'], ['$class': 'RequesterRecipientProvider']]])
    }

    def "[mailer.groovy] sendEmail_failedPR with additional subject"() {
        setup:
        groovyScript.getBinding().setVariable("ghprbPullId", '1')
        groovyScript.getBinding().setVariable("ghprbGhRepository", '2')
        groovyScript.getBinding().setVariable("ghprbPullTitle", '3')
        groovyScript.getBinding().setVariable("BUILD_URL", 'https://redhat.com/')
        when:
        groovyScript.sendEmail_failedPR('additional subject')
        then:
        1 * getPipelineMock("emailext")(['subject': 'additional subject #1 of 2: 3 failed', body: '''
                   Pull request #1 of 2: 3 FAILED
                   Build log: https://redhat.com/consoleText
                   Failed tests ${TEST_COUNTS,var="fail"}: https://redhat.com/testReport
                   (IMPORTANT: For visiting the links you need to have access to Red Hat VPN. In case you don't have access to RedHat VPN please download and decompress attached file.)
                   ''', 'attachmentsPattern': "error.log.gz", 'recipientProviders': [['$class': 'DevelopersRecipientProvider'], ['$class': 'RequesterRecipientProvider']]])
    }

    def "[mailer.groovy] sendEmail_failedPR without additional subject"() {
        setup:
        groovyScript.getBinding().setVariable("ghprbPullId", '1')
        groovyScript.getBinding().setVariable("ghprbGhRepository", '2')
        groovyScript.getBinding().setVariable("ghprbPullTitle", '3')
        groovyScript.getBinding().setVariable("BUILD_URL", 'https://redhat.com/')
        when:
        groovyScript.sendEmail_failedPR()
        then:
        1 * getPipelineMock("emailext")(['subject': 'PR #1 of 2: 3 failed', body: '''
                   Pull request #1 of 2: 3 FAILED
                   Build log: https://redhat.com/consoleText
                   Failed tests ${TEST_COUNTS,var="fail"}: https://redhat.com/testReport
                   (IMPORTANT: For visiting the links you need to have access to Red Hat VPN. In case you don't have access to RedHat VPN please download and decompress attached file.)
                   ''', 'attachmentsPattern': 'error.log.gz', 'recipientProviders': [['$class': 'DevelopersRecipientProvider'], ['$class': 'RequesterRecipientProvider']]])

    }

    def "[mailer.groovy] sendEmail_unstablePR with additional subject"() {
        setup:
        groovyScript.getBinding().setVariable("ghprbPullId", '1')
        groovyScript.getBinding().setVariable("ghprbGhRepository", '2')
        groovyScript.getBinding().setVariable("ghprbPullTitle", '3')
        groovyScript.getBinding().setVariable("BUILD_URL", 'https://redhat.com/')
        when:
        groovyScript.sendEmail_unstablePR('additional subject')
        then:
        1 * getPipelineMock("emailext")(['subject': 'additional subject #1 of 2: 3 was unstable', body: '''
                   Pull request #1 of 2: 3 was UNSTABLE
                   Build log: https://redhat.com/consoleText
                   Failed tests ${TEST_COUNTS,var="fail"}: https://redhat.com/testReport
                   (IMPORTANT: For visiting the links you need to have access to Red Hat VPN)
                   ***********************************************************************************************************************************************************
                   ${FAILED_TESTS}
                   ''', 'recipientProviders': [['$class': 'DevelopersRecipientProvider'], ['$class': 'RequesterRecipientProvider']]])
    }

    def "[mailer.groovy] sendEmail_unstablePR without additional subject"() {
        setup:
        groovyScript.getBinding().setVariable("ghprbPullId", '1')
        groovyScript.getBinding().setVariable("ghprbGhRepository", '2')
        groovyScript.getBinding().setVariable("ghprbPullTitle", '3')
        groovyScript.getBinding().setVariable("BUILD_URL", 'https://redhat.com/')
        when:
        groovyScript.sendEmail_unstablePR()
        then:
        1 * getPipelineMock("emailext")(['subject': 'PR #1 of 2: 3 was unstable', body: '''
                   Pull request #1 of 2: 3 was UNSTABLE
                   Build log: https://redhat.com/consoleText
                   Failed tests ${TEST_COUNTS,var="fail"}: https://redhat.com/testReport
                   (IMPORTANT: For visiting the links you need to have access to Red Hat VPN)
                   ***********************************************************************************************************************************************************
                   ${FAILED_TESTS}
                   ''', 'recipientProviders': [['$class': 'DevelopersRecipientProvider'], ['$class': 'RequesterRecipientProvider']]])
    }

    def "[mailer.groovy] sendEmail_fixedPR with additional subject"() {
        setup:
        groovyScript.getBinding().setVariable("ghprbPullId", '1')
        groovyScript.getBinding().setVariable("ghprbGhRepository", '2')
        groovyScript.getBinding().setVariable("ghprbPullTitle", '3')
        when:
        groovyScript.sendEmail_fixedPR("additional subject")
        then:
        1 * getPipelineMock("emailext")(['subject': 'additional subject #1 of 2: 3 is fixed and was SUCCESSFUL', 'body': '', 'recipientProviders': [['$class': 'DevelopersRecipientProvider'], ['$class': 'RequesterRecipientProvider']]])
    }

    def "[mailer.groovy] sendEmail_fixedPR without additional subject"() {
        setup:
        groovyScript.getBinding().setVariable("ghprbPullId", '1')
        groovyScript.getBinding().setVariable("ghprbGhRepository", '2')
        groovyScript.getBinding().setVariable("ghprbPullTitle", '3')
        when:
        groovyScript.sendEmail_fixedPR()
        then:
        1 * getPipelineMock("emailext")(['subject': 'PR #1 of 2: 3 is fixed and was SUCCESSFUL', 'body': '', 'recipientProviders': [['$class': 'DevelopersRecipientProvider'], ['$class': 'RequesterRecipientProvider']]])
    }

    def "[mailer.groovy] sendEmail_abortedPR with additional subject"() {
        setup:
        groovyScript.getBinding().setVariable("ghprbPullId", '1')
        groovyScript.getBinding().setVariable("ghprbGhRepository", '2')
        groovyScript.getBinding().setVariable("ghprbPullTitle", '3')
        when:
        groovyScript.sendEmail_abortedPR("additional subject")
        then:
        1 * getPipelineMock("emailext")(['subject': 'additional subject #1 of 2: 3 was ABORTED', 'body': '', 'recipientProviders': [['$class': 'DevelopersRecipientProvider'], ['$class': 'RequesterRecipientProvider']]])
    }

    def "[mailer.groovy] sendEmail_abortedPR without additional subject"() {
        setup:
        groovyScript.getBinding().setVariable("ghprbPullId", '1')
        groovyScript.getBinding().setVariable("ghprbGhRepository", '2')
        groovyScript.getBinding().setVariable("ghprbPullTitle", '3')
        when:
        groovyScript.sendEmail_abortedPR()
        then:
        1 * getPipelineMock("emailext")(['subject': 'PR #1 of 2: 3 was ABORTED', 'body': '', 'recipientProviders': [['$class': 'DevelopersRecipientProvider'], ['$class': 'RequesterRecipientProvider']]])
    }

    def "[mailer.groovy] build Log Script PR"() {
        when:
        groovyScript.buildLogScriptPR()
        then:
        1 * getPipelineMock("sh")('touch trace.sh')
        1 * getPipelineMock("sh")('chmod 755 trace.sh')
        1 * getPipelineMock("sh")('echo "wget --no-check-certificate ${BUILD_URL}consoleText" >> trace.sh')
        1 * getPipelineMock("sh")('echo "tail -n 750 consoleText >> error.log" >> trace.sh')
        1 * getPipelineMock("sh")('echo "gzip error.log" >> trace.sh')
    }
}