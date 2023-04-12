
/**
 * Search for Jiras that are CVE from a specific product and version
 *
 * @param productName the product name
 * @param productTargetRelease the product target release version
 * @param jiraUrl the jira search url
 * @param jiraToken the jira token required to authenticate
 * @return an InputStream with the results of the search
 * @throws Exception in case the search fails
 */
def getCVEsFromRelease(String productName, String productTargetRelease, String jiraUrl, String jiraToken) {
    def connection = jiraUrl.toURL().openConnection() as HttpURLConnection
    connection.setRequestMethod('POST')
    connection.addRequestProperty('Authorization', "Bearer ${jiraToken}")
    connection.setRequestProperty('Content-Type', 'application/json')
    connection.setRequestProperty('charset', 'utf-8')
    connection.setDoOutput(true)

    def urlParameters = "{ \"jql\" : \"project=${productName} & \\\"Target Release\\\"=${productTargetRelease} & \\\"CDW release\\\"=\\\"+\\\" & " +
            "labels in (\\\"Security\\\", \\\"SecurityTracking\\\")\", \"maxResults\":1000, \"fields\":[\"key\",\"summary\",\"description\"] }"

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