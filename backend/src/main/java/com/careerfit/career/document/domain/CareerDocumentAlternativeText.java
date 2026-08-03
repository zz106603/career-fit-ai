package com.careerfit.career.document.domain;

import com.careerfit.identity.UserId;
import java.time.Instant;
import java.util.Objects;

public record CareerDocumentAlternativeText(
        CareerDocumentAnalysisId analysisId,
        CareerDocumentId documentId,
        UserId userId,
        String text,
        int textLength,
        String checksumSha256,
        Instant createdAt) {

    public static final int MAX_LENGTH = 200_000;

    public CareerDocumentAlternativeText {
        Objects.requireNonNull(analysisId, "문서 분석 ID는 필수입니다.");
        Objects.requireNonNull(documentId, "경력 문서 ID는 필수입니다.");
        Objects.requireNonNull(userId, "사용자 ID는 필수입니다.");
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("대체 텍스트는 공백일 수 없습니다.");
        }
        if (text.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("대체 텍스트 줄바꿈은 LF여야 합니다.");
        }
        if (textLength != text.codePointCount(0, text.length()) || textLength > MAX_LENGTH) {
            throw new IllegalArgumentException("대체 텍스트 길이가 올바르지 않습니다.");
        }
        if (checksumSha256 == null || !checksumSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("대체 텍스트 checksum 형식이 올바르지 않습니다.");
        }
        Objects.requireNonNull(createdAt, "대체 텍스트 생성 시각은 필수입니다.");
    }
}
