package com.careerfit.career.document.domain;

import com.careerfit.identity.UserId;
import java.time.Instant;
import java.util.Objects;

public record CareerDocument(
        CareerDocumentId id,
        UserId userId,
        String originalName,
        String storageReference,
        long byteSize,
        String contentType,
        String checksumSha256,
        int pageCount,
        Instant uploadedAt,
        Instant deletedAt) {

    public CareerDocument {
        Objects.requireNonNull(id, "경력 문서 ID는 필수입니다.");
        Objects.requireNonNull(userId, "사용자 ID는 필수입니다.");
        requireText(originalName, "원본 파일명은 필수입니다.");
        requireText(storageReference, "저장 참조는 필수입니다.");
        if (byteSize < 1 || byteSize > 10L * 1024 * 1024) {
            throw new IllegalArgumentException("PDF 크기는 1 byte 이상 10 MiB 이하여야 합니다.");
        }
        if (!"application/pdf".equals(contentType)) {
            throw new IllegalArgumentException("검증된 PDF 콘텐츠 유형만 저장할 수 있습니다.");
        }
        if (checksumSha256 == null || !checksumSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("SHA-256 체크섬 형식이 올바르지 않습니다.");
        }
        if (pageCount < 1 || pageCount > 50) {
            throw new IllegalArgumentException("PDF 페이지 수는 1~50이어야 합니다.");
        }
        Objects.requireNonNull(uploadedAt, "업로드 시각은 필수입니다.");
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
