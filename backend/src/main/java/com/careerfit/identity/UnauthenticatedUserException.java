package com.careerfit.identity;

public class UnauthenticatedUserException extends RuntimeException {

    public UnauthenticatedUserException() {
        super("인증된 사용자 컨텍스트가 없습니다.");
    }
}
