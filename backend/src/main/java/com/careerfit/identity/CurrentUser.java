package com.careerfit.identity;

import java.util.Objects;

/** 인증 경계가 확인한 현재 사용자다. */
public record CurrentUser(UserId userId) {

    public CurrentUser {
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
    }
}
