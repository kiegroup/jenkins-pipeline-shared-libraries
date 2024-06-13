import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class JiraSpec extends JenkinsPipelineSpecification {
    def groovyScript = null

    def setup() {
        groovyScript = loadPipelineScriptForTest('vars/jira.groovy')
    }

    def "[jira.groovy] getCVEsFromRelease"() {
        setup:
        def productName = 'RHPAM'
        def targetRelease = '7.12.0.GA'
        def jiraUrl = 'https://issues.redhat.com/rest/api/2/search'
        def jiraToken = ''

        when:
        def input = groovyScript.getCVEsFromRelease(productName, targetRelease, jiraUrl, jiraToken)
        then:
        input instanceof InputStream
    }

    def "[jira.groovy] getCVEsFromRelease exception"() {
        setup:
        def productName = 'RHPAM'
        def targetRelease = '7.12.0.GA'
        def jiraUrl = 'https://foo.bar.com'
        def jiraToken = ''

        when:
        groovyScript.getCVEsFromRelease(productName, targetRelease, jiraUrl, jiraToken)
        then:
        thrown Exception
    }


}