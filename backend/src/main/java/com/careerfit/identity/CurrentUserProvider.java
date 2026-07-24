package com.careerfit.identity;

/** 업무 서비스에 현재 사용자를 제공하는 인증 독립 경계다. */
public interface CurrentUserProvider {

    CurrentUser currentUser();

    default UserId currentUserId() {
        return currentUser().userId();
    }
}
