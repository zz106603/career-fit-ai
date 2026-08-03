package com.careerfit.career.document.application;

public class AlternativeTextException extends RuntimeException {

    private final AlternativeTextFailure failure;

    public AlternativeTextException(AlternativeTextFailure failure, String message) {
        super(message);
        this.failure = failure;
    }

    public AlternativeTextFailure failure() {
        return failure;
    }
}
