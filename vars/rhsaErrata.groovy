import groovy.json.JsonSlurper
import java.util.regex.Pattern

class CVE {
    String name
    String impact
    String jiraNumber
    String bzNumber
    String problemDescription

    static def impactOrdering = [Critical: '1', Important: '2', Moderate: '3', Low: '4']
}

/**
 * Parse a Json that is resulted from getting CVE Jiras.
 * Note that this json must contains summary and description fields.
 *
 * @param cveJson a Json that contains CVE Jiras
 * @param bzLink the Bugzilla link
 * @return a list of CVE
 */
def getCVEList(String cveJson, String bzLink) {
    def resultMap = new JsonSlurper().parseText(cveJson)

    def cveList = []
    resultMap.issues.key.eachWithIndex {jiraNumber, index ->
        def summary = resultMap.issues.fields.summary[index]
        def description = resultMap.issues.fields.description[index]

        // skip an issue if not matching CVE template: summary starting with 'CVE-XXXX..'
        if (summary.trim().startsWith("CVE")) {
            try {
                def cve = new CVE()
                cve.name = getCVENumber(summary)
                cve.impact = getCVEImpact(description)
                cve.jiraNumber = jiraNumber
                cve.bzNumber = getBZNumber(description, bzLink)
                cve.problemDescription = getProblemDescription(summary)
                cveList.add(cve)
            } catch(Exception e) {
                println "[ERROR] Unable to extract information: ${e.getStackTrace()}."
                println "[WARNING] Skipping jira ${jiraNumber}.."
            }
        } else {
            println "[INFO] Skipping Jira ${jiraNumber} since the Jira summary doesn't match the expected format."
        }
    }
    return cveList
}

/**
 * Print a report containing the fields that are expected to be part of a RHSA errata.
 * The report can be copied and pasted to the errata.
 *
 * @param cveList a list of CVE
 * @param cveClassificationLink link for Red Hat CVE severity ratings
 */
def printRHSAReport(List<CVE> cveList, String cveClassificationLink) {
    println '===== RSHA Errata Report ====='

    println "\n- Impact: ${getHigherCVEImpact(cveList)}"

    println "\n- References: ${getReferenceLink(cveList, cveClassificationLink)}"

    println '\n- CVE Names:'
    def cveNames = []
    cveNames.addAll(cveList.name)
    cveNames.sort()
    println cveNames.join(' ')

    println '\n- Bugs or JIRA Issues Fixed:'
    def bugsAndJiras = []
    bugsAndJiras.addAll(cveList.bzNumber)
    bugsAndJiras.addAll(cveList.jiraNumber)
    bugsAndJiras.sort()
    println bugsAndJiras.join(' ')

    println '\n- Problem Description:'
    println 'Security Fix(es):\n'
    println sortProblemDescriptions(cveList).join('\n\n')
}

/**
 * Get the reference link used in the Errata.
 * The link will always point to the higher CVE released as part of the Errata
 *
 * @param cveList a list of CVE
 * @param cveClassificationLink the link for Red Hat CVE severity ratings
 * @return the reference link
 */
def getReferenceLink(List<CVE> cveList, String cveClassificationLink) {
    return "${cveClassificationLink}/#${getHigherCVEImpact(cveList).toLowerCase()}"
}

/**
 * Get the higher CVE impact.
 *
 * @param cveList a list of CVE
 * @return the higher CVE impact
 */
def getHigherCVEImpact(List<CVE> cveList) {
    def impactNumber = cveList.inject([]) {acc, cve ->
        acc.add(CVE.impactOrdering.get(cve.impact))
        acc
    }.sort()
    return CVE.impactOrdering.find {it.value == impactNumber[0]}?.key
}

/**
 * Get the CVE number.
 *
 * @param summary field as part of the CVE Json
 * @return CVE number
 */
def getCVENumber(String summary) {
    return summary.split(' ', 2)[0];
}

/**
 * Get the Bugzilla number.
 *
 * @param description field as part of the CVE Json
 * @param bzLink the Bugzilla link
 * @return Bugzilla number
 */
def getBZNumber(String description, String bzLink) {
    def bzIdLink = description.findAll("${Pattern.quote("${bzLink}/show_bug.cgi?id=")}[0-9]*")[0]
    return bzIdLink.split('=')[1]
}

/**
 * Get the impact of a CVE.
 *
 * @param description field as part of the CVE Json
 * @return CVE impact
 */
def getCVEImpact(String description) {
    def impact = description.findAll('Impact: [A-Za-z]*')[0]
    return impact.split(':')[1].trim()
}

/**
 * Get the problem description.
 * Note that the problem description is already formatted in the correct format expected by the Errata.
 *
 * @param summary field as part of the CVE Json
 * @return problem description
 */
def getProblemDescription(String summary) {
    def firstIndex = summary.indexOf(' ')
    def lastIndex = summary.lastIndexOf(' ');
    def problemDescription = summary.substring(firstIndex, lastIndex).trim()
    def cveNumber = getCVENumber(summary)
    return "* ${problemDescription} (${cveNumber})"
}

/**
 * Sort problem descriptions by CVE impact and also alphabetically.
 * This needed to be implemented this way as Jenkins Groovy CPS fails to get properly sort from a class (compareTo method)
 *
 * @param a list of CVE
 * @return sorted list of problem descriptions
 */
def sortProblemDescriptions(List<CVE> cveList) {
    Map<String, List<String>> problemDescriptions = cveList.inject([:]) {acc, cve ->
        def cveImpactOrdering = CVE.impactOrdering.get(cve.impact)
        def description = acc.get(cveImpactOrdering) ?: []
        description.add(cve.problemDescription)
        Collections.sort(description, String.CASE_INSENSITIVE_ORDER)
        acc.put(cveImpactOrdering, description)
        acc
    }.sort()

    return problemDescriptions.inject([]){list, key, descriptions ->
        descriptions.each {list << it}
        list
    }
}