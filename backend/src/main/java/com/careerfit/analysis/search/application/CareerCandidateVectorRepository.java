package com.careerfit.analysis.search.application;

import com.careerfit.analysis.search.domain.CareerSearchCandidate;
import com.careerfit.identity.UserId;
import java.util.List;

public interface CareerCandidateVectorRepository {

    List<CareerSearchCandidate> searchActiveIndexed(
            UserId userId, List<Double> queryEmbedding, int limit);
}
