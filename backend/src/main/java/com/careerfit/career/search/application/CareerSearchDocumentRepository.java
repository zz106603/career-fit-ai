package com.careerfit.career.search.application;

import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.search.domain.CareerSearchDocument;
import com.careerfit.identity.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CareerSearchDocumentRepository {

    void savePending(CareerSearchDocument document);

    Optional<CareerSearchDocument> findByExperienceVersion(
            UserId userId, CareerExperienceVersionId experienceVersionId);

    boolean markIndexed(
            UserId userId,
            CareerExperienceVersionId experienceVersionId,
            List<Double> embedding,
            String embeddingVersion,
            Instant indexedAt);
}
