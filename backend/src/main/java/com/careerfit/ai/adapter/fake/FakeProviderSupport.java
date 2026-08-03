package com.careerfit.ai.adapter.fake;

import com.careerfit.ai.port.error.ProviderErrorType;
import com.careerfit.ai.port.error.ProviderException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class FakeProviderSupport {

    private FakeProviderSupport() {}

    static void verifyBehavior(FakeProviderBehavior behavior) {
        if (behavior == FakeProviderBehavior.TIMEOUT) {
            throw new ProviderException(ProviderErrorType.TIMEOUT, "Fake Provider timeout");
        }
        if (behavior == FakeProviderBehavior.INVALID_RESPONSE) {
            throw new ProviderException(
                    ProviderErrorType.INVALID_RESPONSE, "Fake Provider invalid response");
        }
        if (behavior == FakeProviderBehavior.RATE_LIMIT) {
            throw new ProviderException(ProviderErrorType.RATE_LIMIT, "Fake Provider rate limit");
        }
        if (behavior == FakeProviderBehavior.PROVIDER_ERROR) {
            throw new ProviderException(ProviderErrorType.PROVIDER_ERROR, "Fake Provider error");
        }
        if (behavior == FakeProviderBehavior.POLICY_REJECTION) {
            throw new ProviderException(ProviderErrorType.POLICY_REJECTION, "Fake Provider policy rejection");
        }
        if (behavior == FakeProviderBehavior.CONFIGURATION_ERROR) {
            throw new ProviderException(
                    ProviderErrorType.CONFIGURATION_ERROR, "Fake Provider configuration error");
        }
    }

    static byte[] digest(String input) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    static String identifier(String input) {
        byte[] digest = digest(input);
        StringBuilder identifier = new StringBuilder(16);
        for (int index = 0; index < 8; index++) {
            identifier.append(String.format("%02x", digest[index]));
        }
        return identifier.toString();
    }
}
