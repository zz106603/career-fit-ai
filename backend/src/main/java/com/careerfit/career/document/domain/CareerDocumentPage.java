package com.careerfit.career.document.domain;

import com.careerfit.identity.UserId;
import java.util.Objects;

public record CareerDocumentPage(
        CareerDocumentAnalysisId analysisId,
        CareerDocumentId documentId,
        UserId userId,
        int pageNumber,
        String text,
        int textLength,
        String checksumSha256) {

    public CareerDocumentPage {
        Objects.requireNonNull(analysisId, "문서 분석 ID는 필수입니다.");
        Objects.requireNonNull(documentId, "경력 문서 ID는 필수입니다.");
        Objects.requireNonNull(userId, "사용자 ID는 필수입니다.");
        if (pageNumber < 1) throw new IllegalArgumentException("페이지 번호는 1 이상이어야 합니다.");
        Objects.requireNonNull(text, "페이지 텍스트는 null일 수 없습니다.");
        if (textLength != text.length()) throw new IllegalArgumentException("텍스트 길이가 일치하지 않습니다.");
        if (checksumSha256 == null || !checksumSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("페이지 체크섬 형식이 올바르지 않습니다.");
        }
    }
}
