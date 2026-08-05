package com.careerfit.career.extraction.web;

import com.careerfit.career.extraction.application.CareerCandidateReviewService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/career-candidates")
/** AI가 만든 미확정 경력 후보의 수정·거절·병합·분리 API를 제공한다. */
public class CareerCandidateController {
    private final CareerCandidateReviewService service;

    public CareerCandidateController(CareerCandidateReviewService service) {
        this.service = service;
    }

    @PatchMapping("/{candidateId}")
    public CareerCandidateResponse edit(
            @PathVariable UUID candidateId, @RequestBody CareerCandidateContentRequest request) {
        return CareerCandidateResponse.from(service.edit(candidateId, request.toContent()));
    }

    @DeleteMapping("/{candidateId}")
    public ResponseEntity<Void> reject(@PathVariable UUID candidateId) {
        service.reject(candidateId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/merges")
    public CareerCandidateResponse merge(@RequestBody CareerCandidateMergeRequest request) {
        if (request.content() == null) throw new IllegalArgumentException("병합 결과 내용은 필수입니다.");
        return CareerCandidateResponse.from(
                service.merge(request.candidateIds(), request.content().toContent()));
    }

    @PostMapping("/{candidateId}/splits")
    public List<CareerCandidateResponse> split(
            @PathVariable UUID candidateId, @RequestBody CareerCandidateSplitRequest request) {
        if (request.contents() == null) throw new IllegalArgumentException("분리 결과 내용은 필수입니다.");
        return service.split(candidateId,
                        request.contents().stream().map(CareerCandidateContentRequest::toContent).toList())
                .stream().map(CareerCandidateResponse::from).toList();
    }
}
