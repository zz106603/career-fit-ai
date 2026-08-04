package com.careerfit.career.extraction.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.careerfit.ai.structured.application.StructuredOutputException;
import com.careerfit.ai.structured.application.StructuredOutputRequest;
import com.careerfit.ai.structured.application.StructuredOutputValidationException;
import com.careerfit.ai.structured.application.StructuredOutputExecutor;
import com.careerfit.ai.structured.application.StructuredOutputFailure;
import com.careerfit.career.document.domain.CareerDocumentAnalysis;
import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.career.document.domain.CareerDocumentId;
import com.careerfit.career.document.domain.CareerDocumentPage;
import com.careerfit.common.async.domain.JobExecutionId;
import com.careerfit.identity.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

@DisplayName("경력 후보 AI 추출 서비스 테스트")
class CareerCandidateExtractionServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-04T00:00:00Z");
    private final StructuredOutputExecutor executor = mock(StructuredOutputExecutor.class);
    private final CareerCandidateExtractionPersistence persistence =
            mock(CareerCandidateExtractionPersistence.class);
    private final CareerCandidateExtractionService service = new CareerCandidateExtractionService(
            executor, persistence, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("원문 페이지에 없는 발췌를 schema 검증 실패로 거부한다")
    void 원문에_없는_발췌를_거부한다() throws Exception {
        ArgumentCaptor<StructuredOutputRequest<?>> captor = ArgumentCaptor.forClass(StructuredOutputRequest.class);
        when(executor.execute(any())).thenThrow(new StructuredOutputException(
                UUID.randomUUID(), StructuredOutputFailure.RESPONSE_SCHEMA_INVALID, 1));

        assertThatThrownBy(() -> service.extract(analysis(), List.of(page("실제 원문")), UUID.randomUUID()))
                .isInstanceOf(StructuredOutputException.class);
        verify(executor).execute(captor.capture());

        assertThatThrownBy(() -> captor.getValue().decoder().decode(new ObjectMapper().readTree("""
                {"candidates":[{"candidateType":"EXPERIENCE","organization":null,
                "role":null,"period":null,"description":"임의 경력","pageNumber":1,
                "excerpt":"원문에 없는 문장"}]}
                """))).isInstanceOf(StructuredOutputValidationException.class);
        verify(persistence, never()).save(any(), any(), any());
    }

    @Test
    @DisplayName("AI 실행 실패 시 후보를 부분 저장하지 않는다")
    void AI_실패에는_후보를_저장하지_않는다() {
        when(executor.execute(any())).thenThrow(new StructuredOutputException(
                UUID.randomUUID(), StructuredOutputFailure.PROVIDER_UNAVAILABLE, 1));

        assertThatThrownBy(() -> service.extract(analysis(), List.of(page("실제 원문")), UUID.randomUUID()))
                .isInstanceOf(StructuredOutputException.class);
        verify(persistence, never()).save(any(), any(), any());
    }

    private CareerDocumentAnalysis analysis() {
        return CareerDocumentAnalysis.queued(CareerDocumentAnalysisId.newId(),
                new CareerDocumentId(UUID.randomUUID()), new UserId(UUID.randomUUID()),
                JobExecutionId.newId(), "input-v1", "workflow-v1", NOW).start(NOW);
    }

    private CareerDocumentPage page(String text) {
        CareerDocumentAnalysis analysis = analysis();
        return new CareerDocumentPage(analysis.id(), analysis.documentId(), analysis.userId(),
                1, text, text.length(), "a".repeat(64));
    }
}
