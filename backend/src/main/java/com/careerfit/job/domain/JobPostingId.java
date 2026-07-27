package com.careerfit.job.domain;

import java.util.Objects;
import java.util.UUID;

public record JobPostingId(UUID value) {

    public JobPostingId {
        Objects.requireNonNull(value, "채용공고 ID는 필수입니다.");
    }

    public static JobPostingId newId() {
        return new JobPostingId(UUID.randomUUID());
    }
}
