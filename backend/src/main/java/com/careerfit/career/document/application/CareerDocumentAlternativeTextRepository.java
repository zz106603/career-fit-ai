package com.careerfit.career.document.application;

import com.careerfit.career.document.domain.CareerDocumentAlternativeText;
import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.career.document.domain.CareerDocumentId;
import com.careerfit.identity.UserId;
import java.util.Optional;

public interface CareerDocumentAlternativeTextRepository {

    void save(CareerDocumentAlternativeText alternativeText);

    Optional<CareerDocumentAlternativeText> find(UserId userId, CareerDocumentAnalysisId analysisId);

    Optional<CareerDocumentAlternativeText> findLatest(UserId userId, CareerDocumentId documentId);
}
