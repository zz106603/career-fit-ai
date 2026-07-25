package com.careerfit.career.domain;

import java.util.Objects;
import java.util.UUID;

public record CareerExperienceId(UUID value) {

    public CareerExperienceId {
        Objects.requireNonNull(value, "value는 null일 수 없습니다.");
    }

    public static CareerExperienceId newId() {
        return new CareerExperienceId(UUID.randomUUID());
    }
}
