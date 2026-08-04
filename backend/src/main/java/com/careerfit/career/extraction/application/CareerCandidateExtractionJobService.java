package com.careerfit.career.extraction.application;

import com.careerfit.career.document.domain.CareerDocumentAnalysis;
import com.careerfit.common.async.application.JobExecutionService;
import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobType;
import org.springframework.stereotype.Service;

@Service
public class CareerCandidateExtractionJobService {
    private final JobExecutionService jobs;

    public CareerCandidateExtractionJobService(JobExecutionService jobs) {
        this.jobs = jobs;
    }

    public JobExecution enqueue(CareerDocumentAnalysis analysis) {
        return jobs.create(analysis.userId().value(), JobType.CAREER_CANDIDATE_EXTRACTION,
                analysis.id().value(), analysis.inputVersion(),
                "career-candidates:" + analysis.id().value() + ":career-candidate-v1");
    }
}
