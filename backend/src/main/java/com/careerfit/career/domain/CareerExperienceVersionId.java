package com.careerfit.career.domain;

import java.util.Objects;
import java.util.UUID;

public record CareerExperienceVersionId(UUID value) {

    public CareerExperienceVersionId {
        Objects.requireNonNull(value, "value는 null일 수 없습니다.");
    }

    public static CareerExperienceVersionId newId() {
        return new CareerExperienceVersionId(UUID.randomUUID());
    }
}
