package com.careerfit.career.document.domain;

import com.careerfit.common.async.domain.JobExecutionId;
import com.careerfit.identity.UserId;
import java.time.Instant;
import java.util.Objects;

public record CareerDocumentAnalysis(
        CareerDocumentAnalysisId id,
        CareerDocumentId documentId,
        UserId userId,
        JobExecutionId jobExecutionId,
        CareerDocumentInputKind inputKind,
        CareerDocumentAnalysisStatus status,
        String inputVersion,
        String workflowVersion,
        String extractedTextReference,
        String failureCode,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt) {

    public CareerDocumentAnalysis {
        Objects.requireNonNull(id, "문서 분석 ID는 필수입니다.");
        Objects.requireNonNull(documentId, "경력 문서 ID는 필수입니다.");
        Objects.requireNonNull(userId, "사용자 ID는 필수입니다.");
        Objects.requireNonNull(inputKind, "입력 종류는 필수입니다.");
        if (inputKind == CareerDocumentInputKind.PDF_TEXT) {
            Objects.requireNonNull(jobExecutionId, "PDF 분석 작업 실행 ID는 필수입니다.");
        } else if (jobExecutionId != null) {
            throw new IllegalArgumentException("대체 텍스트 분석은 PDF 추출 작업을 가질 수 없습니다.");
        }
        Objects.requireNonNull(status, "문서 분석 상태는 필수입니다.");
        inputVersion = requireText(inputVersion, "입력 버전");
        workflowVersion = requireText(workflowVersion, "Workflow 버전");
        extractedTextReference = normalize(extractedTextReference);
        failureCode = normalize(failureCode);
        Objects.requireNonNull(createdAt, "생성 시각은 필수입니다.");
        validateState(status, extractedTextReference, failureCode, startedAt, completedAt);
    }

    public static CareerDocumentAnalysis queued(
            CareerDocumentAnalysisId id,
            CareerDocumentId documentId,
            UserId userId,
            JobExecutionId jobExecutionId,
            String inputVersion,
            String workflowVersion,
            Instant createdAt) {
        return new CareerDocumentAnalysis(
                id, documentId, userId, jobExecutionId, CareerDocumentInputKind.PDF_TEXT,
                CareerDocumentAnalysisStatus.QUEUED, inputVersion, workflowVersion,
                null, null, createdAt, null, null);
    }

    public static CareerDocumentAnalysis pastedText(
            CareerDocumentAnalysisId id,
            CareerDocumentId documentId,
            UserId userId,
            String inputVersion,
            String workflowVersion,
            String textReference,
            Instant createdAt) {
        return new CareerDocumentAnalysis(
                id, documentId, userId, null, CareerDocumentInputKind.PASTED_TEXT,
                CareerDocumentAnalysisStatus.PROCESSING, inputVersion, workflowVersion,
                textReference, null, createdAt, createdAt, null);
    }

    public CareerDocumentAnalysis start(Instant startedAt) {
        requireStatus(CareerDocumentAnalysisStatus.QUEUED);
        return copy(CareerDocumentAnalysisStatus.PROCESSING, null, null, startedAt, null);
    }

    public CareerDocumentAnalysis extractionCompleted(String reference) {
        requireStatus(CareerDocumentAnalysisStatus.PROCESSING);
        return copy(CareerDocumentAnalysisStatus.PROCESSING, reference, null, startedAt, null);
    }

    public CareerDocumentAnalysis fail(String code, Instant completedAt) {
        if (status != CareerDocumentAnalysisStatus.QUEUED
                && status != CareerDocumentAnalysisStatus.PROCESSING) {
            throw new IllegalStateException("종료된 문서 분석은 실패 처리할 수 없습니다.");
        }
        return copy(CareerDocumentAnalysisStatus.FAILED, null, code, startedAt, completedAt);
    }

    private CareerDocumentAnalysis copy(
            CareerDocumentAnalysisStatus newStatus,
            String newReference,
            String newFailureCode,
            Instant newStartedAt,
            Instant newCompletedAt) {
        return new CareerDocumentAnalysis(
                id, documentId, userId, jobExecutionId, inputKind, newStatus,
                inputVersion, workflowVersion, newReference, newFailureCode,
                createdAt, newStartedAt, newCompletedAt);
    }

    private void requireStatus(CareerDocumentAnalysisStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("문서 분석 상태 전이가 올바르지 않습니다: " + status);
        }
    }

    private static void validateState(
            CareerDocumentAnalysisStatus status,
            String reference,
            String failureCode,
            Instant startedAt,
            Instant completedAt) {
        switch (status) {
            case QUEUED -> require(startedAt == null && completedAt == null && failureCode == null,
                    "QUEUED 분석에는 시작·완료 시각과 실패 코드가 없어야 합니다.");
            case PROCESSING -> require(startedAt != null && completedAt == null && failureCode == null,
                    "PROCESSING 분석에는 시작 시각만 있어야 합니다.");
            case SUCCEEDED -> require(startedAt != null && completedAt != null && failureCode == null,
                    "SUCCEEDED 분석에는 시작·완료 시각이 필요합니다.");
            case FAILED -> require(completedAt != null && failureCode != null && reference == null,
                    "FAILED 분석에는 완료 시각과 실패 코드가 필요합니다.");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    private static String requireText(String value, String name) {
        String normalized = normalize(value);
        if (normalized == null) throw new IllegalArgumentException(name + "은 필수입니다.");
        return normalized;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
