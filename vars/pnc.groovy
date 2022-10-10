import groovy.json.JsonSlurper

/**
 * Query the PNC api. The base URL must be configured in PNC_API_URL environment variable.
 * @param endpoint PNC endpoint to invoke
 * @param params key-value map of additional parameters
 * @param pageIndex page to retrieve
 */
def queryPNC(String endpoint, Map params, int pageIndex=0) {
    def queryUrl = "${env.PNC_API_URL}/${endpoint}?pageIndex=${pageIndex}&pageSize=200&" + util.serializeQueryParams(params)
    println "[INFO] Querying PNC ${queryUrl}"

    def response = sh( [script: "curl -s -X GET \"Accept: application/json\" \"${queryUrl}\"", returnStdout: true] )
    // println "[DEBUG] Received from PNC: ${response}"
    return new JsonSlurper().parseText(response as String)
}

/**
 * Returns the number of pages returned by an API call
 */
int getPagesNumber(String endpoint, Map params) {
    return queryPNC(endpoint, params).totalPages as int
}

/**
 * Retrieve all the product's milestones from PNC
 * @param productId id of the product in PNC
 */
def getAllMilestonesForProduct(String productId) {
    println "[INFO] Getting all milestones for product ${productId}"
    return queryPNC("products/${productId}/versions", [q: ""])
}

/**
 * Retrieve the latest (i.e., current) product's milestone from PNC
 * @param productId id of the product in PNC
 */
def getCurrentMilestoneForProduct(String productId) {
    println "[INFO] Getting current milestone for product ${productId}"
    def result = getAllMilestonesForProduct(productId)
    return result.content.last().currentProductMilestone
}

/**
 * Retrieve the PNC identifier of a specific product's milestone
 * @param productId id of the product in PNC
 * @param milestone
 */
def getMilestoneId(String productId, String milestone) {
    println "[INFO] Getting ${milestone} milestone id for product ${productId}"

    // TODO  optimize
    def found = getAllMilestonesForProduct(productId)
            .content.find { c -> c.productMilestones.find { it.value.version == milestone }}
            .productMilestones
            .find { it.value.version == milestone }
            .collect { it.value }
            .first()

    return found?.id
}

/**
 * Retrieve the PNC identifier of the latest (i.e., current) product's milestone
 * @param productId id of the product in PNC
 */
def getCurrentMilestoneId(String productId) {
    println "[INFO] Getting current milestone id for product ${productId}"

    def currentMilestone = getCurrentMilestoneForProduct(productId)
    println "[INFO] Current milestone for product ${productId} is ${currentMilestone?.version}"

    return currentMilestone?.id
}

/**
 * Retrieve all builds related to a specific milestone for the provided projects
 * @param milestoneId id of the milestone in PNC (see getMilestoneId)
 * @param projects list of project names
 */
def getBuildsFromMilestoneId(String milestoneId, List<String> projects) {
    def buildsByProjects = [:]

    def endpoint = "product-milestones/${milestoneId}/builds"
    def params = [q: "temporaryBuild==false"]
    def totalPages = getPagesNumber(endpoint, params)
    for (int i=0; i < totalPages; i++) {
        def page = queryPNC(endpoint, params, i)
        for (project in projects) {
            def builds = page.content.findAll{ it.project.name == project && it.status == "SUCCESS" }.attributes.BREW_BUILD_VERSION
            buildsByProjects[project] = builds.sort().last()
        }
    }

    return buildsByProjects
}