import groovy.transform.PackageScope
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException

def class PipelineHelper {
    def steps

    PipelineHelper(steps) {
        this.steps = steps
    }

    public <T extends Exception> void retry(Closure<?> action, int maxAttempts, int timeoutSeconds, Closure<?> errorAction = null, Class<T> exceptionType = null) {
        int counter = 0
        steps.retry(count: maxAttempts) {
            steps.timeout(time: timeoutSeconds, unit: 'SECONDS') {
                try {
                    steps.println "[INFO] Executing ${counter + 1}/${maxAttempts}"
                    action.call()
                } catch (Exception e) {
                    if (e.getClass() == FlowInterruptedException.class || (exceptionType != null && exceptionType.isInstance(e))) {
                        steps.println "[ERROR] Timeout exceeded in ${counter + 1}/${maxAttempts}"
                        steps.error("Failing build because Timeout ${counter + 1}/${maxAttempts}")
                        if (errorAction) {
                            try {
                                errorAction.call()
                            } catch (Exception errorActionException) {
                                steps.println "[ERROR] Error executing error action ${errorActionException.getMessage()}"
                                throw errorActionException
                            }
                        }
                    } else {
                        throw e
                    }
                } finally {
                    counter++
                }
            }
        }
    }
}
