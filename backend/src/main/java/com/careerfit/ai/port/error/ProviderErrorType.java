package com.careerfit.ai.port.error;

/** 외부 Provider의 제품별 오류를 애플리케이션이 이해하는 유형으로 분류한다. */
public enum ProviderErrorType {
    TIMEOUT,
    RATE_LIMIT,
    PROVIDER_ERROR,
    INVALID_RESPONSE,
    POLICY_REJECTION
}
