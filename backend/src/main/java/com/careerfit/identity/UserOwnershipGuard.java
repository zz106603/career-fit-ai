package com.careerfit.identity;

import java.util.Objects;
import org.springframework.stereotype.Component;

/** 사용자 소유 리소스에 현재 사용자의 범위를 강제한다. */
@Component
public final class UserOwnershipGuard {

    private final CurrentUserProvider currentUserProvider;

    public UserOwnershipGuard(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    public void verifyOwner(UserId ownerId) {
        Objects.requireNonNull(ownerId, "ownerId는 null일 수 없습니다.");
        if (!currentUserProvider.currentUserId().equals(ownerId)) {
            throw new ResourceOwnershipException();
        }
    }
}
