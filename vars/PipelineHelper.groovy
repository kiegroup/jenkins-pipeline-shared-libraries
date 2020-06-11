import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException

def class PipelineHelper {
    def steps

    PipelineHelper(steps) {
        this.steps = steps
    }

    void retry(Closure<?> action, int maxAttempts, int timeoutSeconds, Closure<?> errorAction = null) {
        int counter = 0
        steps.retry(count: maxAttempts) {
            steps.timeout(time: timeoutSeconds, unit: 'SECONDS') {
                try {
                    steps.println "[INFO] Executing ${counter + 1}/${maxAttempts}"
                    action.call();
                } catch (FlowInterruptedException e) {
                    steps.println "[ERROR] Timeout exceeded in ${counter + 1}/${maxAttempts}"
                    steps.error("Failing build because Timeout ${counter + 1}/${maxAttempts}")
                    if(errorAction) {
                        try {
                            errorAction.call();
                        } catch(Exception errorActionException) {
                            steps.println "[ERROR] Error executing error action ${errorActionException.getMessage()}"
                            throw errorActionException
                        }
                    }
                } finally {
                    counter++
                }
            }
        }
    }
}
