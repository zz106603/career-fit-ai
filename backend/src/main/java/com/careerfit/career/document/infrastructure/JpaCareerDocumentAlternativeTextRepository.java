package com.careerfit.career.document.infrastructure;

import com.careerfit.career.document.application.CareerDocumentAlternativeTextRepository;
import com.careerfit.career.document.domain.CareerDocumentAlternativeText;
import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.career.document.domain.CareerDocumentId;
import com.careerfit.identity.UserId;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaCareerDocumentAlternativeTextRepository
        implements CareerDocumentAlternativeTextRepository {

    private final SpringDataCareerDocumentAlternativeTextRepository repository;

    public JpaCareerDocumentAlternativeTextRepository(
            SpringDataCareerDocumentAlternativeTextRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(CareerDocumentAlternativeText value) {
        repository.saveAndFlush(new CareerDocumentAlternativeTextEntity(
                value.analysisId().value(), value.documentId().value(), value.userId().value(),
                value.text(), value.textLength(), value.checksumSha256(), value.createdAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CareerDocumentAlternativeText> find(
            UserId userId, CareerDocumentAnalysisId analysisId) {
        return repository.findByAnalysisIdAndUserId(analysisId.value(), userId.value())
                .map(entity -> new CareerDocumentAlternativeText(
                        new CareerDocumentAnalysisId(entity.analysisId()),
                        new CareerDocumentId(entity.documentId()),
                        new UserId(entity.userId()),
                        entity.text(), entity.textLength(), entity.checksumSha256(),
                        entity.createdAt()));
    }
}
