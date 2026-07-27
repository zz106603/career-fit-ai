package com.careerfit.career.search.domain;

import java.util.Objects;
import java.util.UUID;

public record CareerSearchDocumentId(UUID value) {

    public CareerSearchDocumentId {
        Objects.requireNonNull(value, "value는 null일 수 없습니다.");
    }

    public static CareerSearchDocumentId newId() {
        return new CareerSearchDocumentId(UUID.randomUUID());
    }
}
