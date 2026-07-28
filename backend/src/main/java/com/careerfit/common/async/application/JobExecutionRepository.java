package com.careerfit.common.async.application;

import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobExecutionId;
import com.careerfit.common.async.domain.JobExecutionStatus;
import java.util.Optional;
import java.util.UUID;

public interface JobExecutionRepository {

    JobExecution createOrFindActive(JobExecution queuedExecution);

    Optional<JobExecution> findById(UUID userId, JobExecutionId executionId);

    boolean update(JobExecution execution, JobExecutionStatus expectedStatus);
}
