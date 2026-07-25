package com.careerfit.career.domain;

import com.careerfit.identity.UserId;
import java.time.Instant;
import java.util.Objects;

/** 여러 불변 버전이 속하는 논리 경력이다. */
public record CareerExperience(
        CareerExperienceId id, UserId userId, Instant createdAt, Instant deletedAt) {

    public CareerExperience {
        Objects.requireNonNull(id, "id는 null일 수 없습니다.");
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
        Objects.requireNonNull(createdAt, "createdAt은 null일 수 없습니다.");
    }
}
