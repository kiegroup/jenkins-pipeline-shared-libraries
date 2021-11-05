import groovy.json.JsonSlurper
import org.yaml.snakeyaml.Yaml

/*
* It gets buildChain verion from composite action.yml file
*/
def getBuildChainVersionFromCompositeActionFile(String actionFilePath = '.ci/actions/buildChain/action.yml', String usesContainingString = 'github-action-build@') {
    def actionFileContent = readFile actionFilePath
    def actionObject = new Yaml().load(actionFileContent)

    def uses = actionObject.runs.steps.uses
    def action = uses != null ? uses.find({ it.contains(usesContainingString) }) : null
    if (action == null) {
        throw new RuntimeException("There's not steps with 'uses' for build-chain ${usesContainingString}")
    }

    def buildChainScmRevision = action.substring(action.indexOf('@') + 1)
    try {
        def url = new URL("https://raw.githubusercontent.com/kiegroup/github-action-build-chain/${buildChainScmRevision}/package.json")
        def packageJson = url.getText()
        return new JsonSlurper().parseText(packageJson).version
    } catch(FileNotFoundException e) {
        throw new RuntimeException("There's not scmRevision '${buildChainScmRevision}' for kiegroup/github-action-build-chain repository")
    }
}
