package com.careerfit.common.async.domain;

import java.util.Objects;
import java.util.UUID;

public record JobExecutionId(UUID value) {

    public JobExecutionId {
        Objects.requireNonNull(value, "작업 실행 ID는 필수입니다.");
    }

    public static JobExecutionId newId() {
        return new JobExecutionId(UUID.randomUUID());
    }
}
