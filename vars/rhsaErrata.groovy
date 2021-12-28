import groovy.json.JsonSlurper

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
 * @return a list of CVE
 */
def getCVEList(String cveJson) {
    def resultMap = new JsonSlurper().parseText(cveJson)

    def cveList = []
    def jirasNumber = resultMap.issues.key
    jirasNumber.eachWithIndex {jiraNumber, index ->
        def summary = resultMap.issues.fields.summary[index]
        def description = resultMap.issues.fields.description[index]

        def cve = new CVE()
        cve.name = getCVENumber(summary)
        cve.impact = getCVEImpact(description)
        cve.jiraNumber = jiraNumber
        cve.bzNumber = getBZNumber(description)
        cve.problemDescription = getProblemDescription(summary)
        cveList.add(cve)
    }
    return cveList
}

/**
 * Print a report containing the fields that are expected to be part of a RHSA errata.
 * The report can be copied and pasted to the errata.
 *
 * @param cveList a list of CVE
 */
def printRHSAReport(List<CVE> cveList) {
    println '===== RSHA Errata Report ====='

    println "\n- Impact: ${getHigherCVEImpact(cveList)}"

    println "\n- References: ${getReferenceLink(cveList)}"

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
 * @return the reference link
 */
def getReferenceLink(List<CVE> cveList) {
    def link = 'https://access.redhat.com/security/updates/classification/#'
    return "${link}${getHigherCVEImpact(cveList).toLowerCase()}"
}

/**
 * Get the higher CVE impact.
 *
 * @param cveList a list of CVE
 * @return the higher CVE impact
 */
def getHigherCVEImpact(List<CVE> cveList) {
    def impactNumber = []
    cveList.each {cve -> impactNumber.add(CVE.impactOrdering.get(cve.impact))}
    impactNumber.sort()
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
 * @return Bugzilla number
 */
def getBZNumber(String description) {
    def bzLink = description.findAll('bugzilla.redhat.com\\/show_bug.cgi\\?id=[0-9]*')[0]
    return bzLink.split('=')[1]
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
    Map<String, List<String>> problemDescriptions = [:]
    cveList.each {cve ->
        def cveImpactOrdering = CVE.impactOrdering.get(cve.impact)
        def description = (problemDescriptions.get(cveImpactOrdering) != null) ? problemDescriptions.get(cveImpactOrdering) : []
        description.add(cve.problemDescription)
        Collections.sort(description, String.CASE_INSENSITIVE_ORDER)
        problemDescriptions.put(cveImpactOrdering, description)
    }
    problemDescriptions = problemDescriptions.sort()

    def sortedDescriptions = problemDescriptions.inject([]){list, key, descriptions ->
        descriptions.each {list << it}
        return list
    }
    return sortedDescriptions
}