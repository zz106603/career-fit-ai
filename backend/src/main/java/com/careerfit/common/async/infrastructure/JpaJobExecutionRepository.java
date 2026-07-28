package com.careerfit.common.async.infrastructure;

import com.careerfit.common.async.application.JobExecutionRepository;
import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobExecutionId;
import com.careerfit.common.async.domain.JobExecutionStatus;
import com.careerfit.common.async.domain.JobType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class JpaJobExecutionRepository implements JobExecutionRepository {

    private static final List<JobExecutionStatus> ACTIVE_STATUSES =
            List.of(JobExecutionStatus.QUEUED, JobExecutionStatus.PROCESSING);

    private final SpringDataJobExecutionRepository repository;

    public JpaJobExecutionRepository(SpringDataJobExecutionRepository repository) {
        this.repository = repository;
    }

    @Override
    public JobExecution createOrFindActive(JobExecution execution) {
        int inserted = repository.insertQueued(
                execution.id().value(),
                execution.userId(),
                execution.type().name(),
                execution.targetId(),
                execution.inputVersion(),
                execution.duplicateKey(),
                execution.createdAt());
        if (inserted == 1) {
            return execution;
        }
        return repository
                .findByUserIdAndDuplicateKeyAndStatusIn(
                        execution.userId(), execution.duplicateKey(), ACTIVE_STATUSES)
                .map(this::toDomain)
                .orElseThrow(() ->
                        new IllegalStateException("진행 중인 중복 작업을 조회할 수 없습니다."));
    }

    @Override
    public Optional<JobExecution> findById(UUID userId, JobExecutionId executionId) {
        return repository
                .findByIdAndUserId(executionId.value(), userId)
                .map(this::toDomain);
    }

    @Override
    public List<JobExecution> findQueued(int batchSize) {
        return repository
                .findByStatusOrderByCreatedAtAscIdAsc(
                        JobExecutionStatus.QUEUED, PageRequest.of(0, batchSize))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean update(JobExecution execution, JobExecutionStatus expectedStatus) {
        return repository.updateStatus(
                        execution.id().value(),
                        execution.userId(),
                        expectedStatus,
                        execution.status(),
                        execution.failureCode(),
                        execution.claimedAt(),
                        execution.completedAt())
                == 1;
    }

    private JobExecution toDomain(JobExecutionEntity entity) {
        return new JobExecution(
                new JobExecutionId(entity.id()),
                entity.userId(),
                entity.type(),
                entity.targetId(),
                entity.inputVersion(),
                entity.duplicateKey(),
                entity.status(),
                entity.failureCode(),
                entity.createdAt(),
                entity.claimedAt(),
                entity.completedAt());
    }
}
