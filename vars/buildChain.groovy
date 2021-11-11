import groovy.json.JsonSlurper
import org.yaml.snakeyaml.Yaml

/*
* It gets buildChain verion from composite action.yml file
*/
def getBuildChainVersionFromCompositeActionFile(String actionFilePath = '.ci/actions/build-chain/action.yml', String usesContainingString = 'github-action-build-chain@') {
    def actionFileContent = readFile actionFilePath
    def actionObject = new Yaml().load(actionFileContent)

    def buildChainScmRevision = actionObject.inputs['scm-revision'].default
    try {
        def url = new URL("https://raw.githubusercontent.com/kiegroup/github-action-build-chain/${buildChainScmRevision}/package.json")
        def packageJson = url.getText()
        return new JsonSlurper().parseText(packageJson).version
    } catch(FileNotFoundException e) {
        throw new RuntimeException("There's not scmRevision '${buildChainScmRevision}' for kiegroup/github-action-build-chain repository")
    }
}
