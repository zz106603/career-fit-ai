package com.careerfit.job.domain;

import com.careerfit.identity.UserId;
import java.time.Instant;
import java.util.Objects;

/** 사용자가 등록한 채용공고 원문이다. 구조화 결과는 이 객체와 분리해 관리한다. */
public record JobPosting(
        JobPostingId id,
        UserId userId,
        String originalText,
        String titleHint,
        String companyHint,
        Instant registeredAt,
        Instant deletedAt) {

    public JobPosting {
        Objects.requireNonNull(id, "채용공고 ID는 필수입니다.");
        Objects.requireNonNull(userId, "사용자 ID는 필수입니다.");
        if (originalText == null || originalText.isBlank()) {
            throw new IllegalArgumentException("채용공고 본문은 필수입니다.");
        }
        titleHint = normalize(titleHint);
        companyHint = normalize(companyHint);
        Objects.requireNonNull(registeredAt, "등록 시각은 필수입니다.");
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
