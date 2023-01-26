import groovy.json.JsonSlurper
import org.yaml.snakeyaml.Yaml

/*
* It gets buildChain verion from composite action.yml file
*/
def getBuildChainVersionFromCompositeActionFile(String actionFilePath = '.ci/actions/build-chain/action.yml', String usesContainingString = 'github-action-build-chain@') {
    def actionFileContent = readFile actionFilePath
    def actionObject = new Yaml().load(actionFileContent)

    def uses = actionObject.runs.steps.uses
    def action = uses != null ? uses.find({ it.contains(usesContainingString) }) : null
    if (action == null) {
        throw new RuntimeException("There's not steps with 'uses' for build-chain ${usesContainingString}")
    }

    def buildChainScmRevision = action.substring(action.indexOf('@') + 1)
    return "^${buildChainScmRevision}"
}
