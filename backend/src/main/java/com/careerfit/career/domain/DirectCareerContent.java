package com.careerfit.career.domain;

import java.time.LocalDate;

/** 사용자가 직접 입력한 경력 내용이다. */
public record DirectCareerContent(
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
        String technologies) {

    public DirectCareerContent {
        experienceType = normalize(experienceType);
        title = requireText(title, "경험명 또는 프로젝트명");
        organization = normalize(organization);
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
        role = normalize(role);
        responsibilities = normalize(responsibilities);
        problem = normalize(problem);
        action = normalize(action);
        outcome = normalize(outcome);
        technologies = normalize(technologies);
        if (role == null && responsibilities == null) {
            throw new IllegalArgumentException("역할 또는 수행 내용은 필수입니다.");
        }
    }

    public DirectCareerContent(
            String title, String organization, String role, String responsibilities) {
        this(
                null,
                title,
                organization,
                null,
                null,
                role,
                responsibilities,
                null,
                null,
                null,
                null);
    }

    private static String requireText(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + "은 필수입니다.");
        }
        return normalized;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
