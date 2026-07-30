package com.careerfit.identity.account.domain;

import com.careerfit.identity.UserId;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record UserAccount(
        UserId id,
        String email,
        String passwordHash,
        AccountStatus status,
        Instant createdAt) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public UserAccount {
        Objects.requireNonNull(id, "사용자 ID는 필수입니다.");
        email = normalizeEmail(email);
        passwordHash = requireText(passwordHash, "비밀번호 해시");
        Objects.requireNonNull(status, "계정 상태는 필수입니다.");
        Objects.requireNonNull(createdAt, "계정 생성 시각은 필수입니다.");
    }

    public static String normalizeEmail(String email) {
        String normalized = requireText(email, "이메일").toLowerCase(Locale.ROOT);
        if (normalized.length() > 320) {
            throw new IllegalArgumentException("이메일은 320자 이하여야 합니다.");
        }
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("유효한 이메일 형식이어야 합니다.");
        }
        return normalized;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은 필수입니다.");
        }
        return value.trim();
    }
}
