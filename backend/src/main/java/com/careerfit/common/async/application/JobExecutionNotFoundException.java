package com.careerfit.common.async.application;

import com.careerfit.common.async.domain.JobExecutionId;

public class JobExecutionNotFoundException extends RuntimeException {

    public JobExecutionNotFoundException(JobExecutionId executionId) {
        super("작업 실행을 찾을 수 없습니다: " + executionId.value());
    }
}
