package com.careerfit.common.async.application;

import com.careerfit.common.async.domain.InvalidJobExecutionTransitionException;
import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobExecutionId;
import com.careerfit.common.async.domain.JobExecutionStatus;
import com.careerfit.common.async.domain.JobType;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 비동기 작업의 생성과 상태 전이를 관리하며 업무 Handler의 실행 내용은 알지 않는다. */
public class JobExecutionService {

    private final JobExecutionRepository repository;
    private final Clock clock;

    public JobExecutionService(JobExecutionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public JobExecution create(
            UUID userId,
            JobType type,
            UUID targetId,
            String inputVersion,
            String duplicateKey) {
        JobExecution queued =
                JobExecution.queued(userId, type, targetId, inputVersion, duplicateKey, now());
        return repository.createOrFindActive(queued);
    }

    @Transactional
    public JobExecution start(UUID userId, JobExecutionId executionId) {
        JobExecution current = find(userId, executionId);
        JobExecution processing = current.start(now());
        return update(current.status(), processing);
    }

    @Transactional
    public Optional<JobExecution> claim(JobExecution queuedExecution) {
        JobExecution processing = queuedExecution.start(now());
        if (!repository.update(processing, JobExecutionStatus.QUEUED)) {
            return Optional.empty();
        }
        return Optional.of(processing);
    }

    @Transactional
    public JobExecution succeed(UUID userId, JobExecutionId executionId) {
        JobExecution current = find(userId, executionId);
        JobExecution succeeded = current.succeed(now());
        return update(current.status(), succeeded);
    }

    @Transactional
    public JobExecution fail(UUID userId, JobExecutionId executionId, String failureCode) {
        JobExecution current = find(userId, executionId);
        JobExecution failed = current.fail(failureCode, now());
        return update(current.status(), failed);
    }

    @Transactional(readOnly = true)
    public JobExecution find(UUID userId, JobExecutionId executionId) {
        return repository
                .findById(userId, executionId)
                .orElseThrow(() -> new JobExecutionNotFoundException(executionId));
    }

    @Transactional(readOnly = true)
    public List<JobExecution> findQueued(int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("Dispatcher batch 크기는 1 이상이어야 합니다.");
        }
        return repository.findQueued(batchSize);
    }

    private JobExecution update(JobExecutionStatus expectedStatus, JobExecution target) {
        if (!repository.update(target, expectedStatus)) {
            throw new InvalidJobExecutionTransitionException(expectedStatus, target.status());
        }
        return target;
    }

    private Instant now() {
        return clock.instant();
    }
}
