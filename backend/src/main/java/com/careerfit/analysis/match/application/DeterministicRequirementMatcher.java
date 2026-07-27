package com.careerfit.analysis.match.application;

import com.careerfit.analysis.match.domain.CareerEvidenceSnapshot;
import com.careerfit.analysis.match.domain.RequirementMatchResult;
import com.careerfit.analysis.match.domain.RequirementMatchStatus;
import com.careerfit.analysis.search.domain.CareerCandidateSearch;
import com.careerfit.analysis.search.domain.CareerSearchCandidate;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.DirectCareerContent;
import com.careerfit.job.structure.domain.JobRequirement;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DeterministicRequirementMatcher {

    private static final Pattern YEARS = Pattern.compile("(\\d+)\\s*년");
    private static final Set<String> STOP_WORDS =
            Set.of("경험", "필수", "우대", "이상", "보유", "업무");

    public RequirementMatchResult match(
            JobRequirement requirement,
            CareerCandidateSearch search,
            List<CareerExperienceVersion> versions) {
        if (search.candidates().isEmpty() || versions.isEmpty()) {
            return result(
                    requirement,
                    RequirementMatchStatus.UNKNOWN,
                    "검색된 확정 경력 근거가 없습니다.",
                    null);
        }
        CareerSearchCandidate candidate = search.candidates().getFirst();
        CareerExperienceVersion version = versions.stream()
                .filter(item -> item.id().equals(candidate.experienceVersionId()))
                .findFirst()
                .orElse(null);
        if (version == null) {
            return result(
                    requirement,
                    RequirementMatchStatus.UNKNOWN,
                    "검색 후보의 현재 확정 경력을 확인할 수 없습니다.",
                    null);
        }

        ExplicitConflict conflict = explicitYearsConflict(requirement.text(), version.content());
        if (conflict.conflict()) {
            return result(
                    requirement,
                    RequirementMatchStatus.NOT_SATISFIED,
                    conflict.reason(),
                    evidence(version, candidate, true));
        }

        Set<String> terms = terms(requirement.text());
        long matched = terms.stream()
                .filter(term -> searchableText(version.content()).contains(term))
                .count();
        if (!terms.isEmpty() && matched == terms.size()) {
            return result(
                    requirement,
                    RequirementMatchStatus.SATISFIED,
                    "요구사항의 모든 핵심 단어가 확정 경력에 있습니다.",
                    evidence(version, candidate, false));
        }
        if (matched > 0) {
            return result(
                    requirement,
                    RequirementMatchStatus.PARTIALLY_SATISFIED,
                    "요구사항의 일부 핵심 단어만 확정 경력에서 확인됩니다.",
                    evidence(version, candidate, false));
        }
        return result(
                requirement,
                RequirementMatchStatus.UNKNOWN,
                "확정 경력에서 요구사항을 확인할 근거가 부족합니다.",
                null);
    }

    private RequirementMatchResult result(
            JobRequirement requirement,
            RequirementMatchStatus status,
            String reason,
            CareerEvidenceSnapshot evidence) {
        return new RequirementMatchResult(
                requirement.id(),
                requirement.analysisId(),
                requirement.category(),
                requirement.text(),
                requirement.sourceExcerpt(),
                requirement.sequence(),
                status,
                reason,
                evidence);
    }

    private CareerEvidenceSnapshot evidence(
            CareerExperienceVersion version,
            CareerSearchCandidate candidate,
            boolean explicitConflict) {
        DirectCareerContent content = version.content();
        return new CareerEvidenceSnapshot(
                version.id(),
                version.sourceType(),
                content.title(),
                content.role(),
                content.responsibilities(),
                content.technologies(),
                candidate.score(),
                candidate.rank(),
                explicitConflict);
    }

    private ExplicitConflict explicitYearsConflict(
            String requirementText, DirectCareerContent content) {
        Matcher matcher = YEARS.matcher(requirementText);
        if (!matcher.find() || content.startDate() == null || content.endDate() == null) {
            return new ExplicitConflict(false, null);
        }
        int requiredYears = Integer.parseInt(matcher.group(1));
        long actualMonths = ChronoUnit.MONTHS.between(content.startDate(), content.endDate());
        if (actualMonths < requiredYears * 12L) {
            return new ExplicitConflict(
                    true,
                    "요구 연차 "
                            + requiredYears
                            + "년과 확정 경력 기간 "
                            + actualMonths
                            + "개월이 명시적으로 충돌합니다.");
        }
        return new ExplicitConflict(false, null);
    }

    private Set<String> terms(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}+#.]+"))
                .map(String::trim)
                .filter(term -> term.length() >= 2)
                .filter(term -> !STOP_WORDS.contains(term))
                .filter(term -> !YEARS.matcher(term).matches())
                .collect(Collectors.toSet());
    }

    private String searchableText(DirectCareerContent content) {
        return Arrays.asList(
                        content.title(),
                        content.organization(),
                        content.role(),
                        content.responsibilities(),
                        content.problem(),
                        content.action(),
                        content.outcome(),
                        content.technologies())
                .stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.joining(" "))
                .toLowerCase(Locale.ROOT);
    }

    private record ExplicitConflict(boolean conflict, String reason) {}
}
