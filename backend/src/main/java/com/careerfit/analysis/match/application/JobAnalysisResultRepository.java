package com.careerfit.analysis.match.application;

import com.careerfit.analysis.match.domain.JobAnalysisResult;
import com.careerfit.analysis.match.domain.JobAnalysisResultId;
import com.careerfit.identity.UserId;
import java.util.Optional;

public interface JobAnalysisResultRepository {

    void save(JobAnalysisResult result);

    Optional<JobAnalysisResult> find(UserId userId, JobAnalysisResultId resultId);
}
