package com.careerfit.common.async.application;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobRecoveryScheduler {

    private final JobRecoveryService recoveryService;
    private final boolean enabled;
    private final Duration staleThreshold;
    private final int maxRetries;
    private final int batchSize;

    public JobRecoveryScheduler(
            JobRecoveryService recoveryService,
            @Value("${career-fit.async.recovery.enabled:false}") boolean enabled,
            @Value("${career-fit.async.recovery.stale-threshold:10m}") Duration staleThreshold,
            @Value("${career-fit.async.recovery.max-retries:3}") int maxRetries,
            @Value("${career-fit.async.recovery.batch-size:10}") int batchSize) {
        this.recoveryService = recoveryService;
        this.enabled = enabled;
        this.staleThreshold = staleThreshold;
        this.maxRetries = maxRetries;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${career-fit.async.recovery.fixed-delay:1m}")
    public void recover() {
        if (enabled) {
            recoveryService.recoverStale(staleThreshold, maxRetries, batchSize);
        }
    }
}
