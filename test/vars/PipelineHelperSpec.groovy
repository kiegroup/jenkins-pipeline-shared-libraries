import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import hudson.model.BallColor
import jenkins.model.CauseOfInterruption;
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException
import org.jenkinsci.plugins.workflow.steps.Step
import org.jenkinsci.plugins.workflow.steps.StepContext
import org.jenkinsci.plugins.workflow.steps.StepExecution
import hudson.model.Result;

class PipelineHelperSpec extends JenkinsPipelineSpecification {
    def steps

    def setup() {
        steps = new Step() {
            @Override
            StepExecution start(StepContext stepContext) throws Exception {
                return null
            }
        }
    }

    def "[PipelineHelper.groovy] retry ok"() {
        setup:
        def pipelineHelper = new PipelineHelper(steps)
        when:
        pipelineHelper.retry({
            sh 'ls'
        }, 1, 2)
        then:
        1 * getPipelineMock("sh")('ls')
    }

    def "[PipelineHelper.groovy] retry timeout"() {
        setup:
        def pipelineHelper = new PipelineHelper(steps)

        def caseInterruption = new CauseOfInterruption() {
            String getShortDescription() {
                return "description"
            }
        }
        when:
        pipelineHelper.retry({
            sh 'ls'
            throw new FlowInterruptedException(new Result('result', BallColor.RED, 0, false), caseInterruption)
        }, 1, 1)
        then:
        1 * getPipelineMock("sh")('ls')
        notThrown(FlowInterruptedException)
    }

    def "[PipelineHelper.groovy] different exception"() {
        setup:
        def pipelineHelper = new PipelineHelper(steps)
        when:
        pipelineHelper.retry({
            sh 'ls'
            throw new MissingPropertyException("exception")
        }, 1, 1,  null, MissingPropertyException.class)
        then:
        1 * getPipelineMock("sh")('ls')
        notThrown(MissingPropertyException)
    }

    def "[PipelineHelper.groovy] exception"() {
        setup:
        def pipelineHelper = new PipelineHelper(steps)
        when:
        pipelineHelper.retry({
            sh 'ls'
            throw new Exception('exception')
        }, 1, 1)
        then:
        1 * getPipelineMock("sh")('ls')
        thrown(Exception)
    }
}
