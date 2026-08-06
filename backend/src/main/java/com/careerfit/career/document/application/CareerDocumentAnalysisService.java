package com.careerfit.career.document.application;

import com.careerfit.career.document.domain.CareerDocument;
import com.careerfit.career.document.domain.CareerDocumentAlternativeText;
import com.careerfit.career.document.domain.CareerDocumentAnalysis;
import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.career.document.domain.CareerDocumentId;
import com.careerfit.career.document.domain.CareerDocumentPage;
import com.careerfit.career.extraction.application.CareerCandidateExtractionJobService;
import com.careerfit.common.async.application.JobExecutionService;
import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobType;
import com.careerfit.identity.CurrentUserProvider;
import com.careerfit.identity.UserId;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 문서 분석 이력을 조회하고 기존 결과를 보존한 채 전체 분석을 새 실행으로 시작한다. */
public class CareerDocumentAnalysisService {

    private static final String PASTED_TEXT_REFERENCE =
            "database:career_document_alternative_text";

    private final CareerDocumentRepository documents;
    private final CareerDocumentAnalysisRepository analyses;
    private final CareerDocumentAlternativeTextRepository alternativeTexts;
    private final JobExecutionService jobs;
    private final CareerCandidateExtractionJobService candidateJobs;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public CareerDocumentAnalysisService(
            CareerDocumentRepository documents,
            CareerDocumentAnalysisRepository analyses,
            CareerDocumentAlternativeTextRepository alternativeTexts,
            JobExecutionService jobs,
            CareerCandidateExtractionJobService candidateJobs,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.documents = documents;
        this.analyses = analyses;
        this.alternativeTexts = alternativeTexts;
        this.jobs = jobs;
        this.candidateJobs = candidateJobs;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CareerDocumentAnalysisView> findAll(CareerDocumentId documentId) {
        UserId userId = currentUserProvider.currentUserId();
        requireOwnedDocument(userId, documentId);
        return analyses.findAll(userId, documentId).stream()
                .map(CareerDocumentAnalysisView::from)
                .toList();
    }

    @Transactional
    public CareerDocumentAnalysisView rerun(CareerDocumentId documentId) {
        UserId userId = currentUserProvider.currentUserId();
        CareerDocument document = documents.findActiveForUpdate(userId, documentId)
                .orElseThrow(CareerDocumentNotFoundException::new);
        return analyses.findActive(userId, documentId)
                .map(CareerDocumentAnalysisView::from)
                .orElseGet(() -> alternativeTexts.findLatest(userId, documentId)
                        .map(text -> rerunPastedText(document, text))
                        .orElseGet(() -> rerunPdf(document)));
    }

    private CareerDocumentAnalysisView rerunPdf(CareerDocument document) {
        Instant createdAt = clock.instant();
        CareerDocumentAnalysisId analysisId = CareerDocumentAnalysisId.newId();
        String inputVersion = document.checksumSha256() + ":"
                + CareerDocumentExtractionService.WORKFLOW_VERSION;
        JobExecution job = jobs.create(
                document.userId().value(), JobType.CAREER_DOCUMENT_EXTRACTION,
                analysisId.value(), inputVersion,
                "career-document:" + document.id().value() + ":"
                        + CareerDocumentExtractionService.WORKFLOW_VERSION);
        CareerDocumentAnalysis analysis = CareerDocumentAnalysis.queued(
                analysisId, document.id(), document.userId(), job.id(), inputVersion,
                CareerDocumentExtractionService.WORKFLOW_VERSION, createdAt);
        analyses.save(analysis);
        return CareerDocumentAnalysisView.from(analysis);
    }

    private CareerDocumentAnalysisView rerunPastedText(
            CareerDocument document, CareerDocumentAlternativeText source) {
        Instant createdAt = clock.instant();
        CareerDocumentAnalysis analysis = CareerDocumentAnalysis.pastedText(
                CareerDocumentAnalysisId.newId(), document.id(), document.userId(),
                source.checksumSha256() + ":" + CareerDocumentAlternativeTextService.WORKFLOW_VERSION,
                CareerDocumentAlternativeTextService.WORKFLOW_VERSION,
                PASTED_TEXT_REFERENCE, createdAt);
        CareerDocumentAlternativeText snapshot = new CareerDocumentAlternativeText(
                analysis.id(), document.id(), document.userId(), source.text(),
                source.textLength(), source.checksumSha256(), createdAt);
        analyses.save(analysis);
        alternativeTexts.save(snapshot);
        analyses.savePages(List.of(new CareerDocumentPage(
                analysis.id(), document.id(), document.userId(), 1, source.text(),
                source.text().length(), source.checksumSha256())));
        candidateJobs.enqueue(analysis);
        return CareerDocumentAnalysisView.from(analysis);
    }

    private void requireOwnedDocument(UserId userId, CareerDocumentId documentId) {
        documents.findActive(userId, documentId)
                .orElseThrow(CareerDocumentNotFoundException::new);
    }
}
