package com.careerfit.career.document.application;

import com.careerfit.career.document.domain.CareerDocument;
import com.careerfit.career.document.domain.CareerDocumentId;
import com.careerfit.identity.CurrentUserProvider;
import com.careerfit.identity.UserId;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CareerDocumentService {

    private static final Logger log = LoggerFactory.getLogger(CareerDocumentService.class);
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final CareerDocumentRepository repository;
    private final FileStoragePort fileStorage;
    private final PdfDocumentValidator validator;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public CareerDocumentService(
            CareerDocumentRepository repository,
            FileStoragePort fileStorage,
            PdfDocumentValidator validator,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.repository = repository;
        this.fileStorage = fileStorage;
        this.validator = validator;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public CareerDocument upload(CareerDocumentUpload upload) {
        ValidatedPdf validated = validator.validate(upload);
        UserId userId = currentUserProvider.currentUserId();
        CareerDocumentId documentId = CareerDocumentId.newId();
        String storageReference =
                "career-documents/" + userId.value() + "/" + documentId.value() + ".pdf";
        CareerDocument document = new CareerDocument(
                documentId,
                userId,
                normalizeOriginalName(upload.originalName()),
                storageReference,
                upload.content().length,
                PDF_CONTENT_TYPE,
                validated.checksumSha256(),
                validated.pageCount(),
                clock.instant(),
                null);

        fileStorage.store(storageReference, upload.content());
        try {
            repository.save(document);
        } catch (RuntimeException exception) {
            try {
                fileStorage.delete(storageReference);
            } catch (RuntimeException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
                log.warn(
                        "career document orphan cleanup failed storageReference={}",
                        storageReference);
            }
            throw exception;
        }
        return document;
    }

    public CareerDocument find(CareerDocumentId documentId) {
        return findOwned(documentId);
    }

    public CareerDocumentContent readContent(CareerDocumentId documentId) {
        CareerDocument document = findOwned(documentId);
        return new CareerDocumentContent(document, fileStorage.read(document.storageReference()));
    }

    private CareerDocument findOwned(CareerDocumentId documentId) {
        return repository
                .findActive(currentUserProvider.currentUserId(), documentId)
                .orElseThrow(CareerDocumentNotFoundException::new);
    }

    private static String normalizeOriginalName(String originalName) {
        if (originalName == null) {
            return "document.pdf";
        }
        String normalized = originalName
                .replace('\\', '_')
                .replace('/', '_')
                .replaceAll("[\\p{Cntrl}]", "")
                .trim();
        if (normalized.isBlank()) {
            return "document.pdf";
        }
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }
}
