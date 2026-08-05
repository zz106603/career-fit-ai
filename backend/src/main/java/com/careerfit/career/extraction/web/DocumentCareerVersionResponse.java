package com.careerfit.career.extraction.web;

import com.careerfit.career.domain.CareerExperienceVersion;
import java.time.Instant;
import java.util.UUID;

/** 생성된 논리 경력과 특정 버전의 식별값·확정 상태를 반환한다. */
public record DocumentCareerVersionResponse(
        UUID experienceId, UUID versionId, int versionNo, String sourceType, Instant confirmedAt) {
    static DocumentCareerVersionResponse from(CareerExperienceVersion version) {
        return new DocumentCareerVersionResponse(version.experienceId().value(), version.id().value(),
                version.versionNo(), version.sourceType().name(), version.confirmedAt());
    }
}
