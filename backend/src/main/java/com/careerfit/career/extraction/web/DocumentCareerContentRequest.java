package com.careerfit.career.extraction.web;

import com.careerfit.career.domain.DirectCareerContent;
import java.time.LocalDate;

/** 후보에 없는 확정 필수값을 사용자가 명시해 DOCUMENT 경력 내용으로 전달한다. */
public record DocumentCareerContentRequest(
        String experienceType, String title, String organization,
        LocalDate startDate, LocalDate endDate, String role, String responsibilities,
        String problem, String action, String outcome, String technologies) {
    DirectCareerContent toContent() {
        return new DirectCareerContent(experienceType, title, organization, startDate, endDate,
                role, responsibilities, problem, action, outcome, technologies);
    }
}
