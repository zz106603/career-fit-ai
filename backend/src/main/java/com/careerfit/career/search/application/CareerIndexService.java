package com.careerfit.career.search.application;

import com.careerfit.ai.port.EmbeddingProviderPort;
import com.careerfit.ai.port.model.EmbeddingRequest;
import com.careerfit.ai.port.model.EmbeddingResponse;
import com.careerfit.career.application.CareerExperienceRepository;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.search.domain.CareerSearchDocument;
import com.careerfit.career.search.domain.CareerSearchIndexStatus;
import com.careerfit.identity.CurrentUserProvider;
import com.careerfit.identity.UserId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
/** 확정된 최신 경력 버전만 검색 문서와 Vector 색인 대상으로 전환한다. */
public class CareerIndexService {

    private final CareerExperienceRepository experienceRepository;
    private final CareerSearchDocumentRepository searchDocumentRepository;
    private final EmbeddingProviderPort embeddingProviderPort;
    private final CareerSearchTextBuilder textBuilder;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public CareerIndexService(
            CareerExperienceRepository experienceRepository,
            CareerSearchDocumentRepository searchDocumentRepository,
            EmbeddingProviderPort embeddingProviderPort,
            CareerSearchTextBuilder textBuilder,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.experienceRepository = experienceRepository;
        this.searchDocumentRepository = searchDocumentRepository;
        this.embeddingProviderPort = embeddingProviderPort;
        this.textBuilder = textBuilder;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public CareerSearchDocument index(CareerExperienceVersionId versionId) {
        UserId userId = currentUserProvider.currentUserId();
        CareerExperienceVersion version = experienceRepository
                .findCurrentConfirmedVersion(userId, versionId)
                .orElseThrow(CareerVersionNotIndexableException::new);
        CareerSearchDocument existing =
                searchDocumentRepository.findByExperienceVersion(userId, versionId).orElse(null);
        if (existing != null && existing.status() == CareerSearchIndexStatus.INDEXED) {
            return existing;
        }

        String searchableText = textBuilder.build(version);

        if (existing == null) {
            searchDocumentRepository.savePending(CareerSearchDocument.pending(
                    userId, versionId, searchableText, sha256(searchableText), clock.instant()));
        }

        EmbeddingResponse response =
                embeddingProviderPort.embed(new EmbeddingRequest(searchableText));
        if (!searchDocumentRepository.markIndexed(
                userId, versionId, response.vector(), response.model(), clock.instant())) {
            throw new IllegalStateException("검색 문서를 INDEXED 상태로 변경하지 못했습니다.");
        }
        return searchDocumentRepository
                .findByExperienceVersion(userId, versionId)
                .orElseThrow(() -> new IllegalStateException("색인된 검색 문서를 찾을 수 없습니다."));
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }
}
