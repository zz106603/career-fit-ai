package com.careerfit.identity;

import org.springframework.stereotype.Component;

/** 한 요청 안에서 확인된 현재 사용자를 서비스 호출까지 전달한다. */
@Component
public final class RequestCurrentUserContext implements CurrentUserProvider {

    private final ThreadLocal<CurrentUser> currentUser = new ThreadLocal<>();

    @Override
    public CurrentUser currentUser() {
        CurrentUser user = currentUser.get();
        if (user == null) {
            throw new UnauthenticatedUserException();
        }
        return user;
    }

    public UserScope bind(CurrentUser user) {
        if (currentUser.get() != null) {
            throw new IllegalStateException("현재 요청에 사용자가 이미 설정되어 있습니다.");
        }
        currentUser.set(user);
        return currentUser::remove;
    }

    @FunctionalInterface
    public interface UserScope extends AutoCloseable {

        @Override
        void close();
    }
}
