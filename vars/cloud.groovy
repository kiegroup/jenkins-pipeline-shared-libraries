/*
* Make a quay.io image public if not already
*/
void makeQuayImagePublic(String namespace, String repository, Map credentials = [ 'token': '', 'usernamePassword': '' ]) {
    if (!isQuayImagePublic(namespace, repository, credentials)
        && !setQuayImagePublic(namespace, repository, credentials)) {
        error "Cannot set image quay.io/${namespace}/${repository} as visible"
    }
}

/*
* Checks whether a quay image is public
*/
boolean isQuayImagePublic(String namespace, String repository, Map credentials = [ 'token': '', 'usernamePassword': '' ]) {
    def output = 'false'
    util.executeWithCredentialsMap(credentials) {
        output = sh(returnStdout: true, script: "curl -H 'Authorization: Bearer ${QUAY_TOKEN}' -X GET https://quay.io/api/v1/repository/${namespace}/${repository} | jq '.is_public'").trim()
    }
    return output == 'true'
}

/*
* Sets a Quay repository as public
*
* return false if any problem occurs
*/
boolean setQuayImagePublic(String namespace, String repository, Map credentials = [ 'token': '', 'usernamePassword': '' ]) {
    def output = 'false'
    util.executeWithCredentialsMap(credentials) {
        output = sh(returnStdout: true, script: "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer ${QUAY_TOKEN}' -X POST --data '{\"visibility\": \"public\"}' https://quay.io/api/v1/repository/${namespace}/${repository}/changevisibility | jq '.success'").trim()
    }
    return output == 'true'
}

/*
* Cleanup all containers and images
*/
void cleanContainersAndImages(String containerEngine = 'podman') {
    sh "${containerEngine} rm -f \$(${containerEngine} ps -a -q) || date"
    sh "${containerEngine} rmi -f \$(${containerEngine} images -q) || date"
}