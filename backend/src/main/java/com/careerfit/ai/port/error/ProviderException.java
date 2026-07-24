package com.careerfit.ai.port.error;

import java.util.Objects;

public class ProviderException extends RuntimeException {

    private final ProviderErrorType errorType;

    public ProviderException(ProviderErrorType errorType, String message) {
        super(message);
        this.errorType = Objects.requireNonNull(errorType, "errorType은 null일 수 없습니다.");
    }

    public ProviderErrorType errorType() {
        return errorType;
    }
}
