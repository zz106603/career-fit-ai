package com.careerfit.common.async.application;

import com.careerfit.common.async.domain.JobExecution;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JobWorker {

    private static final String UNEXPECTED_HANDLER_ERROR = "UNEXPECTED_HANDLER_ERROR";
    private static final String HANDLER_INTERRUPTED = "HANDLER_INTERRUPTED";

    private final JobExecutionService executionService;
    private final JobHandlerRegistry handlerRegistry;

    public JobWorker(
            JobExecutionService executionService, JobHandlerRegistry handlerRegistry) {
        this.executionService = executionService;
        this.handlerRegistry = handlerRegistry;
    }

    public boolean execute(JobExecution candidate) {
        Optional<JobExecution> claimed = executionService.claim(candidate);
        if (claimed.isEmpty()) {
            return false;
        }

        JobExecution processing = claimed.orElseThrow();
        try {
            handlerRegistry.resolve(processing.type()).handle(processing);
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            executionService.fail(
                    processing.userId(), processing.id(), failureCode(exception));
            return true;
        }
        executionService.succeed(processing.userId(), processing.id());
        return true;
    }

    private String failureCode(Exception exception) {
        if (exception instanceof JobHandlerException handlerException) {
            return handlerException.failureCode();
        }
        if (exception instanceof InterruptedException) {
            return HANDLER_INTERRUPTED;
        }
        return UNEXPECTED_HANDLER_ERROR;
    }
}
