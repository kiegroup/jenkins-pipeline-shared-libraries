import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class BuildChainSpec extends JenkinsPipelineSpecification {
    def groovyScript = null

    def setup() {
        groovyScript = loadPipelineScriptForTest("vars/buildChain.groovy")
        explicitlyMockPipelineVariable("out")
    }

    def "[build-chain.groovy] get BuildChain Version From CompositeAction File"() {
        setup:
        def actionContent = new File(getClass().getResource('/build-chain-action.yml').toURI()).text
        def packageJsonContent = new File(getClass().getResource('/build-chain-package.json').toURI()).text
        GroovySpy(URL, global: true, useObjenesis: true)
        def mockURL = GroovyMock(URL)

        when:
        def result = groovyScript.getBuildChainVersionFromCompositeActionFile('buildChain-action.yml')
        then:
        1 * getPipelineMock('readFile')('buildChain-action.yml') >> { return actionContent }
        1 * new URL('https://raw.githubusercontent.com/kiegroup/github-action-build-chain/smcVersionString/package.json') >> mockURL
        1 * mockURL.getText() >> packageJsonContent
        result == 'AMAZING_VERSION'
    }

    def "[build-chain.groovy] get BuildChain Version From CompositeAction File from single action file"() {
        setup:
        def actionContent = new File(getClass().getResource('/build-chain-action-single.yml').toURI()).text
        def packageJsonContent = new File(getClass().getResource('/build-chain-package.json').toURI()).text
        GroovySpy(URL, global: true, useObjenesis: true)
        def mockURL = GroovyMock(URL)

        when:
        def result = groovyScript.getBuildChainVersionFromCompositeActionFile('build-chain-action-single.yml')
        then:
        1 * getPipelineMock('readFile')('build-chain-action-single.yml') >> { return actionContent }
        1 * new URL('https://raw.githubusercontent.com/kiegroup/github-action-build-chain/v2.6.17/package.json') >> mockURL
        1 * mockURL.getText() >> packageJsonContent
        result == 'AMAZING_VERSION'
    }
}
