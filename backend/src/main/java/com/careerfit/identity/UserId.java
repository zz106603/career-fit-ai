package com.careerfit.identity;

import java.util.Objects;
import java.util.UUID;

/** 애플리케이션 전체에서 사용하는 사용자 식별자다. */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "value는 null일 수 없습니다.");
    }
}
