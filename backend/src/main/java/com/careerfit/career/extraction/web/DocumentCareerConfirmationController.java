package com.careerfit.career.extraction.web;

import com.careerfit.career.domain.CareerExperienceId;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.extraction.application.DocumentCareerConfirmationService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
/** 후보 최초 확정과 DOCUMENT 경력의 새 버전 생성·확정을 제공하는 HTTP 진입점이다. */
public class DocumentCareerConfirmationController {
    private final DocumentCareerConfirmationService service;

    public DocumentCareerConfirmationController(DocumentCareerConfirmationService service) {
        this.service = service;
    }

    @PostMapping("/career-candidates/{candidateId}/confirmations")
    public ResponseEntity<DocumentCareerVersionResponse> confirmCandidate(
            @PathVariable UUID candidateId, @RequestBody DocumentCareerContentRequest request) {
        return ResponseEntity.status(201)
                .body(DocumentCareerVersionResponse.from(
                        service.confirmCandidate(candidateId, request.toContent())));
    }

    @PostMapping("/career-experiences/{experienceId}/document-revisions")
    public ResponseEntity<DocumentCareerVersionResponse> revise(
            @PathVariable UUID experienceId, @RequestBody DocumentCareerContentRequest request) {
        return ResponseEntity.status(201)
                .body(DocumentCareerVersionResponse.from(
                        service.revise(new CareerExperienceId(experienceId), request.toContent())));
    }

    @PostMapping("/career-experiences/{experienceId}/versions/{versionId}/confirmations")
    public ResponseEntity<Void> confirmRevision(
            @PathVariable UUID experienceId, @PathVariable UUID versionId) {
        service.confirmRevision(new CareerExperienceId(experienceId),
                new CareerExperienceVersionId(versionId));
        return ResponseEntity.noContent().build();
    }
}
