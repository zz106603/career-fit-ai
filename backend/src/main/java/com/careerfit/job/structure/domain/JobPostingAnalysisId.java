package com.careerfit.job.structure.domain;

import java.util.Objects;
import java.util.UUID;

public record JobPostingAnalysisId(UUID value) {

    public JobPostingAnalysisId {
        Objects.requireNonNull(value, "공고 구조화 ID는 필수입니다.");
    }

    public static JobPostingAnalysisId newId() {
        return new JobPostingAnalysisId(UUID.randomUUID());
    }
}
