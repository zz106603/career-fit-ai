package com.careerfit.career.document.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCareerDocumentRepository
        extends JpaRepository<CareerDocumentEntity, UUID> {

    Optional<CareerDocumentEntity> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
}
