package com.careerfit.common.async.domain;

public class InvalidJobExecutionTransitionException extends IllegalStateException {

    public InvalidJobExecutionTransitionException(
            JobExecutionStatus current, JobExecutionStatus target) {
        super("허용되지 않은 작업 상태 전이입니다: " + current + " -> " + target);
    }
}
