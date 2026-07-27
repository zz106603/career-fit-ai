package com.careerfit.analysis.search.domain;

import com.careerfit.identity.UserId;
import com.careerfit.job.structure.domain.JobRequirementId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record CareerCandidateSearch(
        CareerCandidateSearchId id,
        UserId userId,
        JobRequirementId requirementId,
        String queryEmbeddingVersion,
        String searchVersion,
        Instant searchedAt,
        List<CareerSearchCandidate> candidates) {

    public CareerCandidateSearch {
        Objects.requireNonNull(id, "경력 후보 검색 ID는 필수입니다.");
        Objects.requireNonNull(userId, "사용자 ID는 필수입니다.");
        Objects.requireNonNull(requirementId, "공고 요구사항 ID는 필수입니다.");
        queryEmbeddingVersion = requireText(queryEmbeddingVersion, "요구사항 embedding 버전");
        searchVersion = requireText(searchVersion, "검색 버전");
        Objects.requireNonNull(searchedAt, "검색 시각은 필수입니다.");
        Objects.requireNonNull(candidates, "검색 후보 목록은 필수입니다.");
        candidates = List.copyOf(candidates);
        validateRanks(candidates);
    }

    private static void validateRanks(List<CareerSearchCandidate> candidates) {
        for (int index = 0; index < candidates.size(); index++) {
            if (candidates.get(index).rank() != index + 1) {
                throw new IllegalArgumentException("검색 후보 순위는 1부터 연속되어야 합니다.");
            }
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은 필수입니다.");
        }
        return value.trim();
    }
}
