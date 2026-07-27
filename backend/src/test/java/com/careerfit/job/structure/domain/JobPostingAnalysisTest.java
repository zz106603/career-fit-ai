package com.careerfit.job.structure.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.identity.UserId;
import com.careerfit.job.domain.JobPostingId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("채용공고 구조화 도메인 테스트")
class JobPostingAnalysisTest {

    private static final Instant NOW = Instant.parse("2026-07-27T00:00:00Z");

    @Test
    @DisplayName("유효한 요구사항이 있으면 PROCESSING에서 READY로 전환한다")
    void 유효한_요구사항이_있으면_PROCESSING에서_READY로_전환한다() {
        JobPostingAnalysis processing = processing();
        JobRequirement requirement = requirement(processing.id(), "Java 경험");

        JobPostingAnalysis ready = processing.ready(requirement, NOW);

        assertThat(ready.status()).isEqualTo(JobPostingAnalysisStatus.READY);
        assertThat(ready.requirement()).isEqualTo(requirement);
        assertThat(ready.readyAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("요구사항 없이 READY 상태를 만들 수 없다")
    void 요구사항_없이_READY_상태를_만들_수_없다() {
        JobPostingAnalysis processing = processing();

        assertThatThrownBy(() -> new JobPostingAnalysis(
                        processing.id(),
                        processing.jobPostingId(),
                        processing.userId(),
                        JobPostingAnalysisStatus.READY,
                        processing.companyName(),
                        processing.jobTitle(),
                        processing.workflowVersion(),
                        NOW,
                        NOW,
                        null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("본문이 없는 요구사항은 만들 수 없다")
    void 본문이_없는_요구사항은_만들_수_없다() {
        JobPostingAnalysis processing = processing();

        assertThatThrownBy(() -> requirement(processing.id(), " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private JobPostingAnalysis processing() {
        return JobPostingAnalysis.processing(
                JobPostingId.newId(),
                new UserId(UUID.randomUUID()),
                "커리어핏",
                "백엔드 개발자",
                "fake-v1",
                NOW);
    }

    private JobRequirement requirement(JobPostingAnalysisId analysisId, String text) {
        return new JobRequirement(
                JobRequirementId.newId(),
                analysisId,
                JobRequirementCategory.REQUIRED,
                text,
                "Java 경험",
                1);
    }
}
