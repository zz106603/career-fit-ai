package com.careerfit.career.search.application;

import com.careerfit.career.domain.CareerExperienceVersionId;

/** Provider 호출 전에 확정 경력에서 읽어 둔 불변 색인 입력이다. */
public record CareerIndexingInput(
        CareerExperienceVersionId experienceVersionId, String searchableText) {}
