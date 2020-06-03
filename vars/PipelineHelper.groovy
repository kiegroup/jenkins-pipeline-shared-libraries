package com.kie.jenkins

def class PipelineHelper {
    def steps

    PipelineHelper(steps) {
        this.steps = steps
    }

    void retry(Closure<?> action, int maxAttempts, int timeoutSeconds, int count = 0) {
        steps.echo """
-----------------------------------------------
[INFO] Executing action ${count}/${maxAttempts}
-----------------------------------------------
        """
        try {
            steps.timeout(time: timeoutSeconds, unit: 'SECONDS') {
                action.call();
            }
        } catch (final exception) {
            steps.echo "[ERROR] ${exception.toString()} ${count}/${maxAttempts}"
            if (count < maxAttempts) {
                return retry(action, maxAttempts, timeoutSeconds, count + 1)
            } else {
                steps.echo "[ERROR] Max attempts reached. Will not retry."
                throw exception
            }
        }
    }
}