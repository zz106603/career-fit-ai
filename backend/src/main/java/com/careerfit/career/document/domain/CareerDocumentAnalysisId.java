package com.careerfit.career.document.domain;

import java.util.Objects;
import java.util.UUID;

public record CareerDocumentAnalysisId(UUID value) {

    public CareerDocumentAnalysisId {
        Objects.requireNonNull(value, "문서 분석 ID는 필수입니다.");
    }

    public static CareerDocumentAnalysisId newId() {
        return new CareerDocumentAnalysisId(UUID.randomUUID());
    }
}
