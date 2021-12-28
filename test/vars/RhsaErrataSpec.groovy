import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import groovy.json.JsonSlurper

class RhsaErrataSpec extends JenkinsPipelineSpecification {
    def groovyScript = null
    def cveJson = null
    def summary = null
    def description = null

    def setup() {
        groovyScript = loadPipelineScriptForTest('vars/rhsaErrata.groovy')
        cveJson = new File(getClass().getResource('/jira-cve-type.json').toURI()).text
        def resultMap = new JsonSlurper().parseText(cveJson)
        summary = resultMap.issues.fields.summary[0]
        description = resultMap.issues.fields.description[0]
    }

    def "[jira.groovy] getCVEList"() {
        when:
        def cveList = groovyScript.getCVEList(cveJson)
        then:
        cveList.size == 15
    }

    def "[jira.groovy] getCVENumber"() {
        when:
        def cveNumber = groovyScript.getCVENumber(summary)
        then:
        cveNumber == 'CVE-2021-21351'
    }

    def "[jira.groovy] getCVEImpact"() {
        when:
        def impact = groovyScript.getCVEImpact(description)
        then:
        impact == 'Moderate'
    }

    def "[jira.groovy] getBZNumber"() {
        when:
        def bzNumber = groovyScript.getBZNumber(description)
        then:
        bzNumber == '1942642'
    }

    def "[jira.groovy] getProblemDescription"() {
        when:
        def problemDescription = groovyScript.getProblemDescription(summary)
        then:
        problemDescription == '* xstream: allow a remote attacker to load and execute arbitrary code from a remote host only by manipulating the processed input stream (CVE-2021-21351)'
    }

    def "[jira.groovy] getHigherCVEImpact with Moderate as higher"() {
        setup:
        def cveList = []
        cveList.addAll(createCVEWithImpact('Low'), createCVEWithImpact('Moderate'))

        when:
        def higherImpact = groovyScript.getHigherCVEImpact(cveList)
        then:
        higherImpact == 'Moderate'
    }

    def "[jira.groovy] getHigherCVEImpact with Critical as higher"() {
        setup:
        def cveList = []
        cveList.addAll(createCVEWithImpact('Moderate'), createCVEWithImpact('Critical'),
                createCVEWithImpact('Important'))

        when:
        def higherImpact = groovyScript.getHigherCVEImpact(cveList)
        then:
        higherImpact == 'Critical'
    }

    def "[jira.groovy] getReferenceLink"() {
        setup:
        def cveList = []
        cveList.addAll(createCVEWithImpact('Low'), createCVEWithImpact('Important'))

        when:
        def referenceLink = groovyScript.getReferenceLink(cveList)
        then:
        referenceLink == 'https://access.redhat.com/security/updates/classification/#important'
    }

    def "[jira.groovy] sortProblemDescriptions"() {
        setup:
        def cveList = groovyScript.getCVEList(cveJson)
        def firstPosition = 0
        def lastPosition = 14

        when:
        def sortedProblemDescriptions = groovyScript.sortProblemDescriptions(cveList)
        then:
        sortedProblemDescriptions[firstPosition] == '* xmlgraphics-commons: SSRF due to improper input validation by the XMPParser (CVE-2020-11988)'
        sortedProblemDescriptions[lastPosition] == '* xstream: arbitrary code execution via crafted input stream (CVE-2021-21344)'
    }


    def createCVEWithImpact(String impact) {
        def cve = new CVE()
        cve.impact = impact
        return cve
    }
}