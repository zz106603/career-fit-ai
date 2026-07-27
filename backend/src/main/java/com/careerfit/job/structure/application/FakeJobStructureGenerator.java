package com.careerfit.job.structure.application;

import com.careerfit.ai.port.LlmProviderPort;
import com.careerfit.ai.port.model.LlmRequest;
import com.careerfit.ai.port.model.LlmResponse;
import org.springframework.stereotype.Component;

@Component
public class FakeJobStructureGenerator {

    private final LlmProviderPort llmProviderPort;

    public FakeJobStructureGenerator(LlmProviderPort llmProviderPort) {
        this.llmProviderPort = llmProviderPort;
    }

    public FakeJobStructureResult generate(String originalText) {
        LlmResponse response =
                llmProviderPort.generate(new LlmRequest("채용공고 요구사항 1건 구조화\n" + originalText));
        if (response.content().isBlank()) {
            throw new InvalidJobStructureException();
        }
        String sourceExcerpt = originalText.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .findFirst()
                .orElseThrow(InvalidJobStructureException::new);
        return new FakeJobStructureResult(sourceExcerpt, sourceExcerpt, response.model());
    }
}
