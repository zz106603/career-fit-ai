package com.careerfit.career.domain;

import com.careerfit.identity.UserId;
import java.time.Instant;
import java.util.Objects;

/** 경력의 특정 시점 내용을 보존하는 버전이다. */
/** 사용자가 확정하는 경력 내용의 불변 버전이며 새 분석은 현재 확정 버전만 사용한다. */
public record CareerExperienceVersion(
        CareerExperienceVersionId id,
        CareerExperienceId experienceId,
        UserId userId,
        int versionNo,
        CareerExperienceSourceType sourceType,
        DirectCareerContent content,
        Instant createdAt,
        Instant confirmedAt,
        Instant supersededAt,
        Instant deletedAt) {

    public CareerExperienceVersion {
        Objects.requireNonNull(id, "id는 null일 수 없습니다.");
        Objects.requireNonNull(experienceId, "experienceId는 null일 수 없습니다.");
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
        if (versionNo < 1) {
            throw new IllegalArgumentException("versionNo는 1 이상이어야 합니다.");
        }
        Objects.requireNonNull(sourceType, "sourceType은 null일 수 없습니다.");
        Objects.requireNonNull(content, "content는 null일 수 없습니다.");
        Objects.requireNonNull(createdAt, "createdAt은 null일 수 없습니다.");
    }

    public boolean isConfirmed() {
        return confirmedAt != null;
    }
}
