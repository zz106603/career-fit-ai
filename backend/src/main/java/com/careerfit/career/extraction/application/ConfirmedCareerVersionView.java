package com.careerfit.career.extraction.application;

import com.careerfit.career.domain.CareerExperienceVersion;
import java.util.List;

/** 확정 경력 버전 내용과 해당 시점의 Evidence Snapshot을 함께 전달한다. */
public record ConfirmedCareerVersionView(
        CareerExperienceVersion version, List<ConfirmedCareerEvidenceView> evidences) {}
