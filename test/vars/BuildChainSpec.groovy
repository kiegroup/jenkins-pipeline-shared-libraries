import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import org.yaml.snakeyaml.Yaml

class BuildChainSpec extends JenkinsPipelineSpecification {
    def groovyScript = null

    def setup() {
        groovyScript = loadPipelineScriptForTest("vars/buildChain.groovy")
        explicitlyMockPipelineVariable("out")
    }

    def "[build-chain.groovy] get BuildChain Version From CompositeAction File"() {
        setup:
        def actionYaml = new Yaml().load(new File(getClass().getResource('/build-chain-action.yml').toURI()).text)
        GroovySpy(URL, global: true, useObjenesis: true)
        def mockURL = GroovyMock(URL)

        when:
        def result = groovyScript.getBuildChainVersionFromCompositeActionFile('buildChain-action.yml')
        then:
        1 * getPipelineMock('readYaml')([file: 'buildChain-action.yml']) >> { return actionYaml }
        result == '^smcVersionString'
    }

    def "[build-chain.groovy] get BuildChain Version From CompositeAction File from single action file"() {
        setup:
        def actionYaml = new Yaml().load(new File(getClass().getResource('/build-chain-action-single.yml').toURI()).text)
        GroovySpy(URL, global: true, useObjenesis: true)
        def mockURL = GroovyMock(URL)

        when:
        def result = groovyScript.getBuildChainVersionFromCompositeActionFile('build-chain-action-single.yml')
        then:
        1 * getPipelineMock('readYaml')([file: 'build-chain-action-single.yml']) >> { return actionYaml }
        result == '^v2.6.17'
    }
}
