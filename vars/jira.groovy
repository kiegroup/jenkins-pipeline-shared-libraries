
/**
 * Search for Jiras that are CVE from a specific product and version
 *
 * @param productName the product name
 * @param productVersion the product version
 * @param jiraUrl the jira search url
 * @param jiraToken the jira token required to authenticate
 * @param extraJiras (optional) if a specific CVE Jira is required and it is not part of current query
 * @return an InputStream with the results of the search
 * @throws Exception in case the search fails
 */
def getCVEsFromRelease(String productName, String productVersion, String jiraUrl, String jiraToken, String extraJiras=null) {
    def connection = jiraUrl.toURL().openConnection() as HttpURLConnection
    connection.setRequestMethod('POST')
    connection.addRequestProperty('Authorization', "Bearer ${jiraToken}")
    connection.setRequestProperty('Content-Type', 'application/json')
    connection.setRequestProperty('charset', 'utf-8')
    connection.setDoOutput(true)

    def extraJirasQuery = (extraJiras) ? "OR issue in (${extraJiras})" : ""
    def urlParameters = "{ \"jql\" : \"project=${productName} & \\\"fixVersion\\\"=${productVersion} & " +
            "labels in (\\\"Security\\\", \\\"SecurityTracking\\\") ${extraJirasQuery}\", \"maxResults\":1000, \"fields\":[\"key\",\"summary\",\"description\"] }"

    def os = connection.getOutputStream();
    os.write(urlParameters.getBytes('UTF-8'));
    os.close();

    connection.connect()
    def responseCode = connection.getResponseCode()
    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
        return connection.getInputStream()
    } else {
        throw new Exception("""Server failed to respond.
Response code = ${responseCode} / Response message = ${connection.getResponseMessage()}""")
    }
}