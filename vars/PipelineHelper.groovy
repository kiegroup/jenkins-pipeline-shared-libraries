import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException

def class PipelineHelper {
    def steps

    PipelineHelper(steps) {
        this.steps = steps
    }

    void retry(Closure<?> action, int maxAttempts, int timeoutSeconds, Closure<?> errorAction = null) {
        steps.retry(count: maxAttempts) {
            steps.timeout(time: timeoutSeconds, unit: 'SECONDS') {
                try {
                    action.call();
                } catch (FlowInterruptedException e) {
                    steps.println '[ERROR] Timeout exceeded'
                    steps.error('Failing build because Timeout')
                    if(errorAction) {
                        try {
                            errorAction.call();
                        } catch(Exception errorActionException) {
                            steps.println "[ERROR] Error executing error action ${errorActionException.getMessage()}"
                            throw errorActionException
                        }
                    }
                }
            }
        }
    }
}
