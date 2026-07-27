package com.careerfit.career.search.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.identity.development.DevelopmentUsers;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("경력 검색 문서 테스트")
class CareerSearchDocumentTest {

    private static final String CONTENT_HASH = "a".repeat(64);
    private static final Instant NOW = Instant.parse("2026-07-27T00:00:00Z");

    @Test
    @DisplayName("PENDING 문서는 embedding 없이 생성한다")
    void PENDING_문서는_embedding_없이_생성한다() {
        CareerSearchDocument document = CareerSearchDocument.pending(
                DevelopmentUsers.USER_A.userId(),
                CareerExperienceVersionId.newId(),
                "경험명: 프로젝트",
                CONTENT_HASH,
                NOW);

        assertThat(document.status()).isEqualTo(CareerSearchIndexStatus.PENDING);
        assertThat(document.embedding()).isNull();
    }

    @Test
    @DisplayName("INDEXED 문서의 embedding 차원이 다르면 거절한다")
    void INDEXED_문서의_embedding_차원이_다르면_거절한다() {
        assertThatThrownBy(() -> new CareerSearchDocument(
                        CareerSearchDocumentId.newId(),
                        DevelopmentUsers.USER_A.userId(),
                        CareerExperienceVersionId.newId(),
                        "경험명: 프로젝트",
                        CONTENT_HASH,
                        List.of(0.1, 0.2),
                        "fake-embedding-v1",
                        CareerSearchIndexStatus.INDEXED,
                        NOW,
                        NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INDEXED 문서는 8차원 embedding이 필요합니다.");
    }
}
