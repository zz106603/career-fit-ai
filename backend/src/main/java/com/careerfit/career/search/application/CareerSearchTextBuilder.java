package com.careerfit.career.search.application;

import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.DirectCareerContent;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CareerSearchTextBuilder {

    public String build(CareerExperienceVersion version) {
        DirectCareerContent content = version.content();
        List<String> parts = new ArrayList<>();
        add(parts, "경험명", content.title());
        add(parts, "유형", content.experienceType());
        add(parts, "조직", content.organization());
        add(parts, "기간", period(content));
        add(parts, "역할", content.role());
        add(parts, "수행", content.responsibilities());
        add(parts, "문제", content.problem());
        add(parts, "행동", content.action());
        add(parts, "성과", content.outcome());
        add(parts, "기술", content.technologies());
        return String.join("\n", parts);
    }

    private String period(DirectCareerContent content) {
        if (content.startDate() == null && content.endDate() == null) {
            return null;
        }
        String start = content.startDate() == null ? "" : content.startDate().toString();
        String end = content.endDate() == null ? "" : content.endDate().toString();
        return start + " ~ " + end;
    }

    private void add(List<String> parts, String label, Object value) {
        if (value != null && !value.toString().isBlank()) {
            parts.add(label + ": " + value);
        }
    }
}
