package com.careerfit.career.extraction.application;

import com.careerfit.career.document.application.CareerDocumentAnalysisRepository;
import com.careerfit.career.document.domain.CareerDocumentAnalysis;
import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.career.document.domain.CareerDocumentAnalysisStatus;
import com.careerfit.common.async.application.JobHandler;
import com.careerfit.common.async.application.JobHandlerException;
import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobType;
import com.careerfit.identity.UserId;
import org.springframework.stereotype.Component;

@Component
public class CareerCandidateExtractionJobHandler implements JobHandler {
    static final String FAILURE_CODE = "AI_CANDIDATE_EXTRACTION_FAILED";

    private final CareerDocumentAnalysisRepository analyses;
    private final CareerCandidateExtractionService extractionService;
    private final CareerCandidateExtractionPersistence persistence;

    public CareerCandidateExtractionJobHandler(CareerDocumentAnalysisRepository analyses,
            CareerCandidateExtractionService extractionService,
            CareerCandidateExtractionPersistence persistence) {
        this.analyses = analyses;
        this.extractionService = extractionService;
        this.persistence = persistence;
    }

    @Override
    public JobType type() {
        return JobType.CAREER_CANDIDATE_EXTRACTION;
    }

    @Override
    public void handle(JobExecution execution) {
        UserId userId = new UserId(execution.userId());
        CareerDocumentAnalysisId analysisId = new CareerDocumentAnalysisId(execution.targetId());
        CareerDocumentAnalysis analysis = analyses.find(userId, analysisId)
                .orElseThrow(() -> new JobHandlerException(FAILURE_CODE, "문서 분석을 찾을 수 없습니다."));
        if (analysis.status() != CareerDocumentAnalysisStatus.PROCESSING) {
            throw new JobHandlerException(FAILURE_CODE, "후보 추출 가능한 문서 분석 상태가 아닙니다.");
        }
        try {
            extractionService.extract(analysis, analyses.findPages(userId, analysisId), execution.id().value());
        } catch (RuntimeException exception) {
            persistence.fail(analysis, FAILURE_CODE);
            throw new JobHandlerException(FAILURE_CODE, "AI 경력 후보 추출에 실패했습니다.");
        }
    }
}
