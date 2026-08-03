package com.careerfit.career.document.application;

import com.careerfit.career.document.domain.CareerDocumentAnalysis;
import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.career.document.domain.CareerDocumentId;
import com.careerfit.career.document.domain.CareerDocumentPage;
import com.careerfit.career.document.domain.CareerDocumentInputKind;
import com.careerfit.identity.UserId;
import java.util.List;
import java.util.Optional;

public interface CareerDocumentAnalysisRepository {

    void save(CareerDocumentAnalysis analysis);

    Optional<CareerDocumentAnalysis> find(UserId userId, CareerDocumentAnalysisId analysisId);

    Optional<CareerDocumentAnalysis> findActive(
            UserId userId, CareerDocumentId documentId, String inputVersion);

    Optional<CareerDocumentAnalysis> findLatest(
            UserId userId, CareerDocumentId documentId, CareerDocumentInputKind inputKind);

    void savePages(List<CareerDocumentPage> pages);

    List<CareerDocumentPage> findPages(UserId userId, CareerDocumentAnalysisId analysisId);
}
