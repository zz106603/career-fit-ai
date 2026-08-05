package com.careerfit.common.async.application;

import com.careerfit.common.async.domain.JobExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
/** 대기 중인 작업을 주기적으로 찾아 Worker에 전달하는 비동기 실행 진입점이다. */
public class JobDispatcher {

    private final JobExecutionService executionService;
    private final JobWorker worker;
    private final boolean enabled;
    private final int batchSize;

    public JobDispatcher(
            JobExecutionService executionService,
            JobWorker worker,
            @Value("${career-fit.async.dispatcher.enabled:false}") boolean enabled,
            @Value("${career-fit.async.dispatcher.batch-size:10}") int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("Dispatcher batch 크기는 1 이상이어야 합니다.");
        }
        this.executionService = executionService;
        this.worker = worker;
        this.enabled = enabled;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${career-fit.async.dispatcher.fixed-delay:5s}")
    public void poll() {
        if (enabled) {
            dispatchBatch();
        }
    }

    public int dispatchBatch() {
        int executed = 0;
        for (JobExecution candidate : executionService.findQueued(batchSize)) {
            if (worker.execute(candidate)) {
                executed++;
            }
        }
        return executed;
    }
}
