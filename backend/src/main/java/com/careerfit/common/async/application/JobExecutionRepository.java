package com.careerfit.common.async.application;

import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobExecutionId;
import com.careerfit.common.async.domain.JobExecutionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface JobExecutionRepository {

    JobExecution createOrFindActive(JobExecution queuedExecution);

    Optional<JobExecution> findById(UUID userId, JobExecutionId executionId);

    List<JobExecution> findQueued(int batchSize);

    List<JobExecution> findStaleProcessing(Instant claimedBefore, int batchSize);

    boolean update(JobExecution execution, JobExecutionStatus expectedStatus);
}
