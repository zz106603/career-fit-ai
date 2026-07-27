package com.careerfit.analysis.match.domain;

import java.util.Objects;
import java.util.UUID;

public record JobAnalysisResultId(UUID value) {
    public JobAnalysisResultId {
        Objects.requireNonNull(value, "분석 결과 ID는 필수입니다.");
    }

    public static JobAnalysisResultId newId() {
        return new JobAnalysisResultId(UUID.randomUUID());
    }
}
