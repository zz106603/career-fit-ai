package com.careerfit.career.extraction.application;

import com.careerfit.career.domain.CareerExperienceVersionId;
import java.util.UUID;

/** 확정 경력 버전에 복사된 불변 문서 Evidence의 조회 표현이다. */
public record ConfirmedCareerEvidenceView(
        CareerExperienceVersionId versionId,
        UUID documentId,
        String documentName,
        int pageNumber,
        String excerpt) {}
