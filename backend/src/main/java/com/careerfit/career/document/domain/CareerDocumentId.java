package com.careerfit.career.document.domain;

import java.util.Objects;
import java.util.UUID;

public record CareerDocumentId(UUID value) {

    public CareerDocumentId {
        Objects.requireNonNull(value, "경력 문서 ID는 필수입니다.");
    }

    public static CareerDocumentId newId() {
        return new CareerDocumentId(UUID.randomUUID());
    }
}
