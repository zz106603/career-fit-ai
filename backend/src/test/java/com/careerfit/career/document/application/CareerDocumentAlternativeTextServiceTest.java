package com.careerfit.career.document.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.careerfit.career.document.domain.CareerDocument;
import com.careerfit.career.document.domain.CareerDocumentAlternativeText;
import com.careerfit.career.document.domain.CareerDocumentAnalysis;
import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.career.document.domain.CareerDocumentId;
import com.careerfit.career.document.domain.CareerDocumentInputKind;
import com.careerfit.common.async.domain.JobExecutionId;
import com.careerfit.career.extraction.application.CareerCandidateExtractionJobService;
import com.careerfit.identity.CurrentUserProvider;
import com.careerfit.identity.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("경력 문서 대체 텍스트 서비스 테스트")
class CareerDocumentAlternativeTextServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-03T00:00:00Z");
    private final CareerDocumentRepository documents = mock(CareerDocumentRepository.class);
    private final CareerDocumentAnalysisRepository analyses = mock(CareerDocumentAnalysisRepository.class);
    private final CareerDocumentAlternativeTextRepository alternativeTexts =
            mock(CareerDocumentAlternativeTextRepository.class);
    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final CareerCandidateExtractionJobService candidateJobs =
            mock(CareerCandidateExtractionJobService.class);
    private final UserId userId = new UserId(UUID.randomUUID());
    private final CareerDocumentId documentId = new CareerDocumentId(UUID.randomUUID());
    private CareerDocumentAlternativeTextService service;

    @BeforeEach
    void 서비스를_준비한다() {
        service = new CareerDocumentAlternativeTextService(
                documents, analyses, alternativeTexts, currentUserProvider,
                Clock.fixed(NOW, ZoneOffset.UTC), candidateJobs);
        when(currentUserProvider.currentUserId()).thenReturn(userId);
        when(documents.findActiveForUpdate(userId, documentId)).thenReturn(Optional.of(document()));
        when(analyses.findActive(any(), any(), any())).thenReturn(Optional.empty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PDF_TEXT_EMPTY", "PDF_PARSE_FAILED", "PDF_ENCRYPTED"})
    @DisplayName("콘텐츠 기반 PDF 추출 실패에는 대체 텍스트를 허용한다")
    void 콘텐츠_기반_추출_실패에는_대체_텍스트를_허용한다(String failureCode) {
        when(analyses.findLatest(userId, documentId, CareerDocumentInputKind.PDF_TEXT))
                .thenReturn(Optional.of(failedAnalysis(failureCode)));

        CareerDocumentAlternativeTextResult result = service.create(documentId, "경력\r\n내용");

        assertThat(result.created()).isTrue();
        assertThat(result.inputKind()).isEqualTo(CareerDocumentInputKind.PASTED_TEXT);
        verify(analyses).save(any(CareerDocumentAnalysis.class));
        verify(analyses).savePages(any());
        verify(alternativeTexts).save(any(CareerDocumentAlternativeText.class));
        verify(candidateJobs).enqueue(any(CareerDocumentAnalysis.class));
    }

    @Test
    @DisplayName("저장소 실패 분석에는 대체 텍스트를 허용하지 않는다")
    void 인프라_실패에는_대체_텍스트를_허용하지_않는다() {
        when(analyses.findLatest(userId, documentId, CareerDocumentInputKind.PDF_TEXT))
                .thenReturn(Optional.of(failedAnalysis("FILE_STORAGE_READ_FAILED")));

        assertThatThrownBy(() -> service.create(documentId, "경력 내용"))
                .isInstanceOf(AlternativeTextException.class)
                .extracting("failure")
                .isEqualTo(AlternativeTextFailure.NOT_ALLOWED);
        verify(alternativeTexts, never()).save(any());
    }

    @Test
    @DisplayName("최대 글자 수를 넘으면 문서를 조회하기 전에 거부한다")
    void 최대_글자_수를_넘으면_거부한다() {
        String tooLong = "가".repeat(CareerDocumentAlternativeText.MAX_LENGTH + 1);

        assertThatThrownBy(() -> service.create(documentId, tooLong))
                .isInstanceOf(AlternativeTextException.class)
                .extracting("failure")
                .isEqualTo(AlternativeTextFailure.TOO_LONG);
        verify(documents, never()).findActiveForUpdate(any(), any());
    }

    private CareerDocument document() {
        return new CareerDocument(
                documentId, userId, "resume.pdf", "users/file.pdf", 100,
                "application/pdf", "a".repeat(64), 1, NOW, null);
    }

    private CareerDocumentAnalysis failedAnalysis(String failureCode) {
        return CareerDocumentAnalysis.queued(
                        CareerDocumentAnalysisId.newId(), documentId, userId,
                        JobExecutionId.newId(), "pdf-v1", "pdf-text-extraction-v1", NOW)
                .start(NOW.plusSeconds(1))
                .fail(failureCode, NOW.plusSeconds(2));
    }
}
