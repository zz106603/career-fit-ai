package com.careerfit.common.async.application;

import com.careerfit.common.async.domain.InvalidJobExecutionTransitionException;
import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobExecutionId;
import com.careerfit.common.async.domain.JobExecutionStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 장시간 멈춘 PROCESSING 작업을 재대기 또는 실패로 전환해 비동기 흐름을 복구한다. */
public class JobRecoveryService {

    public static final String STALE_RETRY_EXHAUSTED = "STALE_RETRY_EXHAUSTED";

    private final JobExecutionRepository repository;
    private final Clock clock;

    public JobRecoveryService(JobExecutionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public StaleRecoveryResult recoverStale(
            Duration staleThreshold, int maxRetries, int batchSize) {
        validatePolicy(staleThreshold, maxRetries, batchSize);
        Instant now = clock.instant();
        List<JobExecution> staleExecutions =
                repository.findStaleProcessing(now.minus(staleThreshold), batchSize);
        int requeued = 0;
        int failed = 0;

        for (JobExecution execution : staleExecutions) {
            JobExecution recovered = execution.retryCount() < maxRetries
                    ? execution.requeueStale()
                    : execution.failStale(STALE_RETRY_EXHAUSTED, now);
            if (repository.update(recovered, JobExecutionStatus.PROCESSING)) {
                if (recovered.status() == JobExecutionStatus.QUEUED) {
                    requeued++;
                } else {
                    failed++;
                }
            }
        }
        return new StaleRecoveryResult(requeued, failed);
    }

    @Transactional
    public JobExecution rerunFailed(UUID userId, JobExecutionId executionId) {
        JobExecution failed = repository
                .findById(userId, executionId)
                .orElseThrow(() -> new JobExecutionNotFoundException(executionId));
        if (failed.status() != JobExecutionStatus.FAILED) {
            throw new InvalidJobExecutionTransitionException(
                    failed.status(), JobExecutionStatus.QUEUED);
        }
        JobExecution rerun = JobExecution.queued(
                failed.userId(),
                failed.type(),
                failed.targetId(),
                failed.inputVersion(),
                failed.duplicateKey(),
                clock.instant());
        return repository.createOrFindActive(rerun);
    }

    private static void validatePolicy(
            Duration staleThreshold, int maxRetries, int batchSize) {
        if (staleThreshold == null || staleThreshold.isZero() || staleThreshold.isNegative()) {
            throw new IllegalArgumentException("stale 기준 시간은 0보다 커야 합니다.");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("최대 재시도 횟수는 0 이상이어야 합니다.");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("복구 batch 크기는 1 이상이어야 합니다.");
        }
    }
}
