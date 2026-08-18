package com.careerfit.career.extraction.web;

import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.DirectCareerContent;
import com.careerfit.career.extraction.application.ConfirmedCareerVersionView;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ConfirmedCareerVersionResponse(
        UUID experienceId,
        UUID versionId,
        int versionNo,
        String sourceType,
        boolean current,
        Instant createdAt,
        Instant confirmedAt,
        Instant supersededAt,
        String experienceType,
        String title,
        String organization,
        LocalDate startDate,
        LocalDate endDate,
        String role,
        String responsibilities,
        String problem,
        String action,
        String outcome,
        String technologies,
        List<ConfirmedCareerEvidenceResponse> evidences) {

    static ConfirmedCareerVersionResponse from(ConfirmedCareerVersionView view) {
        CareerExperienceVersion version = view.version();
        DirectCareerContent content = version.content();
        return new ConfirmedCareerVersionResponse(
                version.experienceId().value(), version.id().value(), version.versionNo(),
                version.sourceType().name(), version.supersededAt() == null, version.createdAt(),
                version.confirmedAt(), version.supersededAt(), content.experienceType(),
                content.title(), content.organization(), content.startDate(), content.endDate(),
                content.role(), content.responsibilities(), content.problem(), content.action(),
                content.outcome(), content.technologies(),
                view.evidences().stream().map(ConfirmedCareerEvidenceResponse::from).toList());
    }
}
