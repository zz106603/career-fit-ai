package com.careerfit.career.extraction.web;

import java.util.List;
import java.util.UUID;

public record CareerCandidateMergeRequest(
        List<UUID> candidateIds, CareerCandidateContentRequest content) {}
