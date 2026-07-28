package com.careerfit.common.async.application;

import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobType;

public interface JobHandler {

    JobType type();

    void handle(JobExecution execution) throws Exception;
}
