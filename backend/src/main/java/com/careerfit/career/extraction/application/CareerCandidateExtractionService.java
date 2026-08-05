package com.careerfit.career.extraction.application;

import com.careerfit.ai.structured.application.StructuredOutputRequest;
import com.careerfit.ai.structured.application.StructuredOutputValidationException;
import com.careerfit.ai.structured.application.StructuredOutputExecutor;
import com.careerfit.ai.structured.application.StructuredOutputResult;
import com.careerfit.career.document.domain.CareerDocumentAnalysis;
import com.careerfit.career.document.domain.CareerDocumentPage;
import com.careerfit.career.extraction.domain.CareerExtractionCandidate;
import com.careerfit.career.extraction.domain.CareerExtractionCandidateStatus;
import com.careerfit.career.extraction.domain.ExperienceEvidence;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

@Service
/** AI 출력을 검증한 뒤 미확정 후보와 원문 Evidence를 함께 만드는 추출 유스케이스다. */
public class CareerCandidateExtractionService {
    static final String PROMPT_VERSION = "career-candidate-v1";
    static final String SCHEMA_VERSION = "career-candidate-schema-v1";
    static final String SCHEMA = """
            {"type":"object","properties":{"candidates":{"type":"array","items":{"type":"object","properties":{"candidateType":{"type":"string"},"organization":{"type":["string","null"]},"role":{"type":["string","null"]},"period":{"type":["string","null"]},"description":{"type":"string"},"pageNumber":{"type":"integer","minimum":1},"excerpt":{"type":"string"}},"required":["candidateType","organization","role","period","description","pageNumber","excerpt"],"additionalProperties":false}}},"required":["candidates"],"additionalProperties":false}
            """;

    private final StructuredOutputExecutor executor;
    private final CareerCandidateExtractionPersistence persistence;
    private final Clock clock;

    public CareerCandidateExtractionService(StructuredOutputExecutor executor,
            CareerCandidateExtractionPersistence persistence, Clock clock) {
        this.executor = executor; this.persistence = persistence; this.clock = clock;
    }

    public void extract(CareerDocumentAnalysis analysis, List<CareerDocumentPage> pages,
            UUID workflowExecutionId) {
        Map<Integer, String> pageTexts = new HashMap<>();
        pages.forEach(page -> pageTexts.put(page.pageNumber(), page.text()));
        StructuredOutputResult<List<ExtractedCareerCandidate>> result = executor.execute(
                new StructuredOutputRequest<>(workflowExecutionId, analysis.id().value().toString(),
                        "CAREER_CANDIDATE_EXTRACTION", PROMPT_VERSION, SCHEMA_VERSION,
                        prompt(pages), "career_candidates", SCHEMA, root -> decode(root, pageTexts)));
        List<CareerExtractionCandidate> candidates = new ArrayList<>();
        List<ExperienceEvidence> evidences = new ArrayList<>();
        for (ExtractedCareerCandidate extracted : result.value()) {
            UUID candidateId = UUID.randomUUID();
            candidates.add(new CareerExtractionCandidate(candidateId, analysis.id(), analysis.userId(),
                    extracted.candidateType(), extracted.organization(), extracted.role(), extracted.period(),
                    extracted.description(), CareerExtractionCandidateStatus.PENDING_REVIEW, 1, result.model(),
                    PROMPT_VERSION, SCHEMA_VERSION, result.aiCallExecutionId(), clock.instant()));
            evidences.add(new ExperienceEvidence(UUID.randomUUID(), candidateId, analysis.id(),
                    analysis.documentId(), analysis.userId(), extracted.pageNumber(), extracted.excerpt()));
        }
        persistence.save(analysis, candidates, evidences);
    }

    private List<ExtractedCareerCandidate> decode(JsonNode root, Map<Integer, String> pages)
            throws StructuredOutputValidationException {
        JsonNode values = root.path("candidates");
        if (!values.isArray()) throw new StructuredOutputValidationException("candidates 배열이 필요합니다.");
        List<ExtractedCareerCandidate> result = new ArrayList<>();
        for (JsonNode value : values) {
            int page = value.path("pageNumber").asInt(0);
            String excerpt = text(value, "excerpt", true);
            String pageText = pages.get(page);
            if (pageText == null || !pageText.contains(excerpt)) {
                throw new StructuredOutputValidationException("근거 발췌를 원문 페이지에서 확인할 수 없습니다.");
            }
            result.add(new ExtractedCareerCandidate(text(value, "candidateType", true),
                    text(value, "organization", false), text(value, "role", false),
                    text(value, "period", false), text(value, "description", true), page, excerpt));
        }
        return List.copyOf(result);
    }

    private String text(JsonNode node, String name, boolean required)
            throws StructuredOutputValidationException {
        JsonNode value = node.path(name);
        if (value.isMissingNode() || value.isNull()) {
            if (required) throw new StructuredOutputValidationException(name + "은 필수입니다.");
            return null;
        }
        String text = value.asText().trim();
        if (required && text.isBlank()) throw new StructuredOutputValidationException(name + "은 필수입니다.");
        return text.isBlank() ? null : text;
    }

    private String prompt(List<CareerDocumentPage> pages) {
        StringBuilder prompt = new StringBuilder("문서에 명시된 사실만 경력 후보로 추출하세요. 추론하거나 보완하지 마세요. 각 후보는 원문과 완전히 일치하는 짧은 excerpt와 pageNumber를 포함하세요.\n");
        for (CareerDocumentPage page : pages) {
            prompt.append("\n[PAGE ").append(page.pageNumber()).append("]\n").append(page.text());
        }
        return prompt.toString();
    }
}
