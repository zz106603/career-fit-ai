package com.careerfit.career.extraction.web;

import com.careerfit.career.domain.CareerExperienceId;
import com.careerfit.career.extraction.application.ConfirmedCareerQueryService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/career-experiences")
/** 현재 확정 경력 목록과 경력별 불변 버전 이력을 제공한다. */
public class ConfirmedCareerQueryController {
    private final ConfirmedCareerQueryService service;

    public ConfirmedCareerQueryController(ConfirmedCareerQueryService service) {
        this.service = service;
    }

    @GetMapping
    public List<ConfirmedCareerVersionResponse> findCurrent() {
        return service.findCurrent().stream().map(ConfirmedCareerVersionResponse::from).toList();
    }

    @GetMapping("/{experienceId}")
    public List<ConfirmedCareerVersionResponse> findVersions(@PathVariable UUID experienceId) {
        return service.findVersions(new CareerExperienceId(experienceId)).stream()
                .map(ConfirmedCareerVersionResponse::from)
                .toList();
    }
}
