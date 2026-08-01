package com.careerfit.career.document.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

interface SpringDataCareerDocumentRepository
        extends JpaRepository<CareerDocumentEntity, UUID> {

    Optional<CareerDocumentEntity> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select document from CareerDocumentEntity document "
            + "where document.id = :id and document.userId = :userId and document.deletedAt is null")
    Optional<CareerDocumentEntity> findLockedByIdAndUserIdAndDeletedAtIsNull(
            @Param("id") UUID id, @Param("userId") UUID userId);
}
