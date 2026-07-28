package com.careerfit.analysis.search.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCareerCandidateSearchRepository
        extends JpaRepository<CareerCandidateSearchEntity, UUID> {

    Optional<CareerCandidateSearchEntity> findByIdAndUserId(UUID id, UUID userId);
}
