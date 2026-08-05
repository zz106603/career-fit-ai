package com.careerfit.career.document.application;

import com.careerfit.career.document.domain.CareerDocument;
import com.careerfit.career.document.domain.CareerDocumentAnalysis;
import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.career.document.domain.CareerDocumentId;
import com.careerfit.common.async.application.JobExecutionService;
import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobType;
import com.careerfit.identity.CurrentUserProvider;
import com.careerfit.identity.UserId;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 문서 분석 실행과 비동기 작업을 생성하며, 실제 PDF 처리는 Worker에 위임한다. */
public class CareerDocumentExtractionService {

    static final String WORKFLOW_VERSION = "pdf-text-extraction-v1";
    private final CareerDocumentRepository documents;
    private final CareerDocumentAnalysisRepository analyses;
    private final JobExecutionService jobs;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public CareerDocumentExtractionService(
            CareerDocumentRepository documents,
            CareerDocumentAnalysisRepository analyses,
            JobExecutionService jobs,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.documents = documents;
        this.analyses = analyses;
        this.jobs = jobs;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Transactional
    public CareerDocumentExtractionResult request(CareerDocumentId documentId) {
        UserId userId = currentUserProvider.currentUserId();
        CareerDocument document = documents.findActiveForUpdate(userId, documentId)
                .orElseThrow(CareerDocumentNotFoundException::new);
        String inputVersion = document.checksumSha256() + ":" + WORKFLOW_VERSION;
        return analyses.findActive(userId, documentId, inputVersion)
                .map(this::result)
                .orElseGet(() -> create(document, inputVersion));
    }

    private CareerDocumentExtractionResult create(CareerDocument document, String inputVersion) {
        CareerDocumentAnalysisId analysisId = new CareerDocumentAnalysisId(UUID.randomUUID());
        String duplicateKey = "career-document:" + document.id().value() + ":" + WORKFLOW_VERSION;
        JobExecution job = jobs.create(
                document.userId().value(), JobType.CAREER_DOCUMENT_EXTRACTION,
                analysisId.value(), inputVersion, duplicateKey);
        CareerDocumentAnalysis analysis = CareerDocumentAnalysis.queued(
                analysisId, document.id(), document.userId(), job.id(), inputVersion,
                WORKFLOW_VERSION, clock.instant());
        analyses.save(analysis);
        return result(analysis);
    }

    private CareerDocumentExtractionResult result(CareerDocumentAnalysis analysis) {
        return new CareerDocumentExtractionResult(
                analysis.id(), analysis.jobExecutionId(), analysis.status());
    }
}
