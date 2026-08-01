package com.careerfit.career.document.application;

import com.careerfit.career.document.domain.CareerDocument;
import com.careerfit.career.document.domain.CareerDocumentId;
import com.careerfit.identity.UserId;
import java.util.Optional;

public interface CareerDocumentRepository {

    void save(CareerDocument document);

    Optional<CareerDocument> findActive(UserId userId, CareerDocumentId documentId);

    Optional<CareerDocument> findActiveForUpdate(UserId userId, CareerDocumentId documentId);
}
