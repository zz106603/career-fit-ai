package com.careerfit.career.document.application;

import com.careerfit.career.document.domain.CareerDocument;
import com.careerfit.career.document.domain.CareerDocumentAlternativeText;
import com.careerfit.career.document.domain.CareerDocumentAnalysis;
import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.career.document.domain.CareerDocumentAnalysisStatus;
import com.careerfit.career.document.domain.CareerDocumentId;
import com.careerfit.career.document.domain.CareerDocumentInputKind;
import com.careerfit.career.document.domain.CareerDocumentPage;
import com.careerfit.career.extraction.application.CareerCandidateExtractionJobService;
import com.careerfit.identity.CurrentUserProvider;
import com.careerfit.identity.UserId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareerDocumentAlternativeTextService {

    static final String WORKFLOW_VERSION = "pasted-text-v1";
    private static final String TEXT_REFERENCE = "database:career_document_alternative_text";
    private static final Set<String> ALLOWED_FAILURE_CODES = Set.of(
            "PDF_TEXT_EMPTY", "PDF_PARSE_FAILED", "PDF_ENCRYPTED");

    private final CareerDocumentRepository documents;
    private final CareerDocumentAnalysisRepository analyses;
    private final CareerDocumentAlternativeTextRepository alternativeTexts;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;
    private final CareerCandidateExtractionJobService candidateJobs;

    public CareerDocumentAlternativeTextService(
            CareerDocumentRepository documents,
            CareerDocumentAnalysisRepository analyses,
            CareerDocumentAlternativeTextRepository alternativeTexts,
            CurrentUserProvider currentUserProvider,
            Clock clock,
            CareerCandidateExtractionJobService candidateJobs) {
        this.documents = documents;
        this.analyses = analyses;
        this.alternativeTexts = alternativeTexts;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
        this.candidateJobs = candidateJobs;
    }

    @Transactional
    public CareerDocumentAlternativeTextResult create(
            CareerDocumentId documentId, String requestedText) {
        NormalizedText normalized = normalize(requestedText);
        UserId userId = currentUserProvider.currentUserId();
        CareerDocument document = documents.findActiveForUpdate(userId, documentId)
                .orElseThrow(CareerDocumentNotFoundException::new);
        requireAlternativeTextAllowed(userId, documentId);

        String inputVersion = normalized.checksum() + ":" + WORKFLOW_VERSION;
        return analyses.findActive(userId, documentId, inputVersion)
                .map(analysis -> existingResult(userId, analysis))
                .orElseGet(() -> save(document, normalized, inputVersion));
    }

    private void requireAlternativeTextAllowed(UserId userId, CareerDocumentId documentId) {
        CareerDocumentAnalysis latestPdfAnalysis = analyses
                .findLatest(userId, documentId, CareerDocumentInputKind.PDF_TEXT)
                .orElseThrow(this::notAllowed);
        if (latestPdfAnalysis.status() != CareerDocumentAnalysisStatus.FAILED
                || !ALLOWED_FAILURE_CODES.contains(latestPdfAnalysis.failureCode())) {
            throw notAllowed();
        }
    }

    private CareerDocumentAlternativeTextResult save(
            CareerDocument document, NormalizedText normalized, String inputVersion) {
        Instant createdAt = clock.instant();
        CareerDocumentAnalysis analysis = CareerDocumentAnalysis.pastedText(
                CareerDocumentAnalysisId.newId(), document.id(), document.userId(), inputVersion,
                WORKFLOW_VERSION, TEXT_REFERENCE, createdAt);
        CareerDocumentAlternativeText alternativeText = new CareerDocumentAlternativeText(
                analysis.id(), document.id(), document.userId(), normalized.text(),
                normalized.length(), normalized.checksum(), createdAt);
        try {
            analyses.save(analysis);
            alternativeTexts.save(alternativeText);
            analyses.savePages(List.of(new CareerDocumentPage(
                    analysis.id(), document.id(), document.userId(), 1, normalized.text(),
                    normalized.text().length(), normalized.checksum())));
            candidateJobs.enqueue(analysis);
        } catch (DataAccessException exception) {
            throw new AlternativeTextException(
                    AlternativeTextFailure.SAVE_FAILED, "대체 텍스트 저장에 실패했습니다.");
        }
        return result(analysis, alternativeText, true);
    }

    private CareerDocumentAlternativeTextResult existingResult(
            UserId userId, CareerDocumentAnalysis analysis) {
        CareerDocumentAlternativeText alternativeText = alternativeTexts.find(userId, analysis.id())
                .orElseThrow(() -> new IllegalStateException("대체 텍스트 Snapshot을 찾을 수 없습니다."));
        return result(analysis, alternativeText, false);
    }

    private CareerDocumentAlternativeTextResult result(
            CareerDocumentAnalysis analysis,
            CareerDocumentAlternativeText alternativeText,
            boolean created) {
        return new CareerDocumentAlternativeTextResult(
                analysis.id(), analysis.inputKind(), analysis.status(),
                alternativeText.textLength(), alternativeText.createdAt(), created);
    }

    private NormalizedText normalize(String requestedText) {
        if (requestedText == null) {
            throw empty();
        }
        String text = requestedText.replace("\r\n", "\n").replace('\r', '\n');
        if (text.isBlank()) {
            throw empty();
        }
        int length = text.codePointCount(0, text.length());
        if (length > CareerDocumentAlternativeText.MAX_LENGTH) {
            throw new AlternativeTextException(
                    AlternativeTextFailure.TOO_LONG, "대체 텍스트는 200,000자 이하여야 합니다.");
        }
        return new NormalizedText(text, length, checksum(text));
    }

    private String checksum(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private AlternativeTextException empty() {
        return new AlternativeTextException(
                AlternativeTextFailure.EMPTY, "대체 텍스트는 공백일 수 없습니다.");
    }

    private AlternativeTextException notAllowed() {
        return new AlternativeTextException(
                AlternativeTextFailure.NOT_ALLOWED, "현재 문서는 대체 텍스트 입력 대상이 아닙니다.");
    }

    private record NormalizedText(String text, int length, String checksum) {}
}
