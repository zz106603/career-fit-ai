package com.careerfit.career.document.infrastructure;

import com.careerfit.career.document.application.CareerDocumentRepository;
import com.careerfit.career.document.domain.CareerDocument;
import com.careerfit.career.document.domain.CareerDocumentId;
import com.careerfit.identity.UserId;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaCareerDocumentRepository implements CareerDocumentRepository {

    private final SpringDataCareerDocumentRepository repository;

    public JpaCareerDocumentRepository(SpringDataCareerDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(CareerDocument document) {
        repository.saveAndFlush(new CareerDocumentEntity(
                document.id().value(),
                document.userId().value(),
                document.originalName(),
                document.storageReference(),
                document.byteSize(),
                document.contentType(),
                document.checksumSha256(),
                document.pageCount(),
                document.uploadedAt(),
                document.deletedAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CareerDocument> findActive(
            UserId userId, CareerDocumentId documentId) {
        return repository
                .findByIdAndUserIdAndDeletedAtIsNull(documentId.value(), userId.value())
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public Optional<CareerDocument> findActiveForUpdate(
            UserId userId, CareerDocumentId documentId) {
        return repository
                .findLockedByIdAndUserIdAndDeletedAtIsNull(documentId.value(), userId.value())
                .map(this::toDomain);
    }

    private CareerDocument toDomain(CareerDocumentEntity entity) {
        return new CareerDocument(
                new CareerDocumentId(entity.id()),
                new UserId(entity.userId()),
                entity.originalName(),
                entity.storageReference(),
                entity.byteSize(),
                entity.contentType(),
                entity.checksumSha256(),
                entity.pageCount(),
                entity.uploadedAt(),
                entity.deletedAt());
    }
}
