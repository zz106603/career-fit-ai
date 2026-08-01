package com.careerfit.career.document.application;

import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.career.document.domain.CareerDocumentAnalysisStatus;
import com.careerfit.common.async.domain.JobExecutionId;

public record CareerDocumentExtractionResult(
        CareerDocumentAnalysisId analysisId,
        JobExecutionId jobExecutionId,
        CareerDocumentAnalysisStatus status) {}
