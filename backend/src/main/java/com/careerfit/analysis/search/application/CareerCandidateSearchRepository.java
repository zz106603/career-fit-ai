package com.careerfit.analysis.search.application;

import com.careerfit.analysis.search.domain.CareerCandidateSearch;
import com.careerfit.analysis.search.domain.CareerCandidateSearchId;
import com.careerfit.identity.UserId;
import java.util.Optional;

public interface CareerCandidateSearchRepository {

    void save(CareerCandidateSearch search);

    Optional<CareerCandidateSearch> find(UserId userId, CareerCandidateSearchId searchId);
}
