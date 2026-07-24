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
