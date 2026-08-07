package com.careerfit.career.extraction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.career.document.domain.CareerDocumentId;
import com.careerfit.career.extraction.domain.CareerCandidateContent;
import com.careerfit.career.extraction.domain.CareerExtractionCandidate;
import com.careerfit.career.extraction.domain.CareerExtractionCandidateStatus;
import com.careerfit.career.extraction.domain.ExperienceEvidence;
import com.careerfit.identity.CurrentUser;
import com.careerfit.identity.CurrentUserProvider;
import com.careerfit.identity.UserId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("경력 후보 검토 서비스 테스트")
class CareerCandidateReviewServiceTest {
    private static final UserId USER = new UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private final FakeRepository repository = new FakeRepository();
    private final CurrentUserProvider currentUserProvider = () -> new CurrentUser(USER);
    private final CareerCandidateReviewService service =
            new CareerCandidateReviewService(repository, currentUserProvider);

    @Test
    @DisplayName("후보를 수정하면 EDITED 상태와 다음 revision을 저장하고 근거를 유지한다")
    void 후보를_수정하면_EDITED_상태와_다음_revision을_저장하고_근거를_유지한다() {
        CareerExtractionCandidate source = candidate(USER);
        ExperienceEvidence evidence = evidence(source);
        repository.seed(source, evidence);

        CareerExtractionCandidate edited = service.edit(source.id(), content("수정 내용"));

        assertThat(edited.status()).isEqualTo(CareerExtractionCandidateStatus.EDITED);
        assertThat(edited.revisionNo()).isEqualTo(2);
        assertThat(repository.evidences).containsExactly(evidence);
    }

    @Test
    @DisplayName("후보를 거절하면 물리 삭제하지 않고 REJECTED 상태로 저장한다")
    void 후보를_거절하면_물리_삭제하지_않고_REJECTED_상태로_저장한다() {
        CareerExtractionCandidate source = candidate(USER);
        repository.seed(source, evidence(source));

        service.reject(source.id());

        assertThat(repository.candidates.get(source.id()).status())
                .isEqualTo(CareerExtractionCandidateStatus.REJECTED);
    }

    @Test
    @DisplayName("후보를 병합하면 원본을 거절하고 모든 근거를 새 후보에 보존한다")
    void 후보를_병합하면_원본을_거절하고_모든_근거를_새_후보에_보존한다() {
        CareerExtractionCandidate first = candidate(USER);
        CareerExtractionCandidate second = candidate(USER);
        repository.seed(first, evidence(first));
        repository.seed(second, evidence(second));

        CareerExtractionCandidate merged =
                service.merge(List.of(first.id(), second.id()), content("병합 내용"));

        assertThat(merged.status()).isEqualTo(CareerExtractionCandidateStatus.EDITED);
        assertThat(repository.candidates.get(first.id()).status())
                .isEqualTo(CareerExtractionCandidateStatus.REJECTED);
        assertThat(repository.evidences.stream().filter(e -> e.candidateId().equals(merged.id())))
                .hasSize(2);
    }

    @Test
    @DisplayName("후보를 분리하면 원본을 거절하고 각 결과에 근거를 보존한다")
    void 후보를_분리하면_원본을_거절하고_각_결과에_근거를_보존한다() {
        CareerExtractionCandidate source = candidate(USER);
        repository.seed(source, evidence(source));

        List<CareerExtractionCandidate> results = service.split(
                source.id(), List.of(content("첫 결과"), content("두 번째 결과")));

        assertThat(results).hasSize(2).allMatch(result ->
                result.status() == CareerExtractionCandidateStatus.EDITED);
        assertThat(repository.candidates.get(source.id()).status())
                .isEqualTo(CareerExtractionCandidateStatus.REJECTED);
        assertThat(results).allSatisfy(result -> assertThat(repository.evidences.stream()
                .filter(evidence -> evidence.candidateId().equals(result.id()))).hasSize(1));
    }

    @Test
    @DisplayName("다른 사용자 후보는 찾을 수 없는 후보로 처리한다")
    void 다른_사용자_후보는_찾을_수_없는_후보로_처리한다() {
        CareerExtractionCandidate other = candidate(new UserId(UUID.randomUUID()));
        repository.seed(other, evidence(other));

        assertThatThrownBy(() -> service.edit(other.id(), content("침범")))
                .isInstanceOf(CareerCandidateNotFoundException.class);
    }

    private static CareerCandidateContent content(String description) {
        return new CareerCandidateContent("PROJECT", "커리어핏", "개발", "2026", description);
    }

    private static CareerExtractionCandidate candidate(UserId userId) {
        return new CareerExtractionCandidate(UUID.randomUUID(), new CareerDocumentAnalysisId(UUID.randomUUID()),
                userId, "PROJECT", "커리어핏", "개발", "2026", "원본 내용",
                CareerExtractionCandidateStatus.PENDING_REVIEW, 1, "fake", "v1", "v1",
                UUID.randomUUID(), Instant.parse("2026-08-05T00:00:00Z"));
    }

    private static ExperienceEvidence evidence(CareerExtractionCandidate candidate) {
        return new ExperienceEvidence(UUID.randomUUID(), candidate.id(), candidate.analysisId(),
                new CareerDocumentId(UUID.randomUUID()), candidate.userId(), 1,
                "근거 " + candidate.id());
    }

    private static class FakeRepository implements CareerExtractionCandidateRepository {
        private final Map<UUID, CareerExtractionCandidate> candidates = new HashMap<>();
        private final List<ExperienceEvidence> evidences = new ArrayList<>();

        void seed(CareerExtractionCandidate candidate, ExperienceEvidence evidence) {
            candidates.put(candidate.id(), candidate);
            evidences.add(evidence);
        }

        @Override public boolean exists(UserId userId, CareerDocumentAnalysisId analysisId) { return false; }

        @Override
        public List<CareerExtractionCandidate> findAll(
                UserId userId, CareerDocumentAnalysisId analysisId) {
            return candidates.values().stream()
                    .filter(candidate -> candidate.userId().equals(userId)
                            && candidate.analysisId().equals(analysisId)
                            && candidate.status() != CareerExtractionCandidateStatus.REJECTED)
                    .toList();
        }

        @Override
        public void saveAll(List<CareerExtractionCandidate> values, List<ExperienceEvidence> evidenceValues) {
            values.forEach(value -> candidates.put(value.id(), value));
            evidences.addAll(evidenceValues);
        }

        @Override
        public List<CareerExtractionCandidate> findAllForUpdate(UserId userId, List<UUID> candidateIds) {
            return candidateIds.stream().map(candidates::get)
                    .filter(candidate -> candidate != null && candidate.userId().equals(userId)).toList();
        }

        @Override
        public List<ExperienceEvidence> findEvidences(UserId userId, List<UUID> candidateIds) {
            return evidences.stream().filter(evidence -> evidence.userId().equals(userId)
                    && candidateIds.contains(evidence.candidateId())).toList();
        }

        @Override
        public List<CareerCandidateEvidenceView> findEvidenceViews(
                UserId userId, List<UUID> candidateIds) {
            return List.of();
        }
    }
}
