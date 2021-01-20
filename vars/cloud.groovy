/*
* Make a quay.io image public if not already
*/ 
void makeQuayImagePublic(String namespace, String repository, String quayTokenCredentialsId) {
    if(!isQuayRepositoryPublic(namespace, repository, quayTokenCredentialsId)){
        if(!setQuayImagePublic(namespace, repository, quayTokenCredentialsId)) {
            error "Cannot set image quay.io/${namespace}/${repository} as visible"
        }
    }
}

/*
* Checks whether a quay repository is public
*/
boolean isQuayRepositoryPublic(String namespace, String repository, String quayTokenCredentialsId) {
    def output = 'false'
    withCredentials([string(credentialsId: quayTokenCredentialsId, variable: 'QUAY_TOKEN')]) {
        output = sh(returnStdout: true, script: "curl -H 'Authorization: Bearer ${QUAY_TOKEN}' -X GET https://quay.io/api/v1/repository/${namespace}/${repository} | jq '.is_public'").trim()
    }
    return output == 'true'
}

/*
* Sets a Quay repository as public
*
* return false if any problem occurs
*/
boolean setQuayImagePublic(String namespace, String repository, String quayTokenCredentialsId) {
    def output = 'false'
    withCredentials([string(credentialsId: quayTokenCredentialsId, variable: 'QUAY_TOKEN')]) {
        output = sh(returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer ${QUAY_TOKEN}' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/${namespace}/${repository}/changevisibility | jq '.success'").trim()
    }
    return output == 'true'
}
