package com.careerfit.job.structure.domain;

import java.util.Objects;
import java.util.UUID;

public record JobRequirementId(UUID value) {

    public JobRequirementId {
        Objects.requireNonNull(value, "공고 요구사항 ID는 필수입니다.");
    }

    public static JobRequirementId newId() {
        return new JobRequirementId(UUID.randomUUID());
    }
}
