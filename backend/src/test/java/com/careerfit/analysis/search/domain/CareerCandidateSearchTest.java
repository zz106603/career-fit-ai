package com.careerfit.analysis.search.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.identity.UserId;
import com.careerfit.job.structure.domain.JobRequirementId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("경력 검색 후보 Snapshot 도메인 테스트")
class CareerCandidateSearchTest {

    @Test
    @DisplayName("후보 순위가 1부터 연속되지 않으면 생성할 수 없다")
    void 후보_순위가_1부터_연속되지_않으면_생성할_수_없다() {
        CareerSearchCandidate candidate = new CareerSearchCandidate(
                new CareerExperienceVersionId(UUID.randomUUID()),
                0.8,
                2,
                "fake-embedding-v1");

        assertThatThrownBy(() -> search(List.of(candidate)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("검색 후보가 없어도 빈 Snapshot을 생성할 수 있다")
    void 검색_후보가_없어도_빈_Snapshot을_생성할_수_있다() {
        search(List.of());
    }

    private CareerCandidateSearch search(List<CareerSearchCandidate> candidates) {
        return new CareerCandidateSearch(
                CareerCandidateSearchId.newId(),
                new UserId(UUID.randomUUID()),
                new JobRequirementId(UUID.randomUUID()),
                "fake-embedding-v1",
                "pgvector-cosine-v1",
                Instant.parse("2026-07-27T00:00:00Z"),
                candidates);
    }
}
