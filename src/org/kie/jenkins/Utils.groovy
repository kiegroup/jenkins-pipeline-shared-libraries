package org.kie.jenkins

/*
 * Utils class with some common utils methods for Jenkins pipelines.
 */
class Utils {

    /*
     * Create a temporary dir on the machine (linux)
     */
    static String createTempDir(def script) {
        return script.sh(returnStdout: true, script: 'mktemp -d').trim()
    }

    /*
     * Create a temporary file on the machine (linux)
     */
    static String createTempFile(def script, String content = '') {
        String tmpFile = script.sh(returnStdout: true, script: 'mktemp').trim()
        if (content) {
            script.writeFile(file: tmpFile, text: content)
        }
        return tmpFile
    }
}
