package com.careerfit.common.async.application;

public class JobHandlerException extends RuntimeException {

    private final String failureCode;

    public JobHandlerException(String failureCode, String message) {
        super(message);
        if (failureCode == null || failureCode.isBlank()) {
            throw new IllegalArgumentException("Handler 실패 코드는 필수입니다.");
        }
        this.failureCode = failureCode.trim();
    }

    public String failureCode() {
        return failureCode;
    }
}
