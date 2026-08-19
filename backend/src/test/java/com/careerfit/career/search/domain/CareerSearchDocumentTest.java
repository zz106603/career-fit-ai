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
                        NOW,
                        NOW,
                        null,
                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INDEXED 문서는 8차원 embedding이 필요합니다.");
    }

    @Test
    @DisplayName("PENDING 문서는 INDEXING을 거쳐 INDEXED로 전이한다")
    void PENDING_문서는_INDEXING을_거쳐_INDEXED로_전이한다() {
        CareerSearchDocument pending = pending();

        CareerSearchDocument indexing = pending.start(NOW.plusSeconds(1));
        CareerSearchDocument indexed = indexing.succeed(
                List.of(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8),
                "fake-embedding-v1",
                NOW.plusSeconds(2));

        assertThat(indexing.status()).isEqualTo(CareerSearchIndexStatus.INDEXING);
        assertThat(indexed.status()).isEqualTo(CareerSearchIndexStatus.INDEXED);
        assertThat(indexed.indexingStartedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(indexed.indexedAt()).isEqualTo(NOW.plusSeconds(2));
    }

    @Test
    @DisplayName("INDEXING 문서는 실패 코드와 시각을 기록하며 FAILED로 전이한다")
    void INDEXING_문서는_실패_코드와_시각을_기록하며_FAILED로_전이한다() {
        CareerSearchDocument failed = pending()
                .start(NOW.plusSeconds(1))
                .fail("CAREER_INDEXING_FAILED", NOW.plusSeconds(2));

        assertThat(failed.status()).isEqualTo(CareerSearchIndexStatus.FAILED);
        assertThat(failed.failureCode()).isEqualTo("CAREER_INDEXING_FAILED");
        assertThat(failed.failedAt()).isEqualTo(NOW.plusSeconds(2));
    }

    @Test
    @DisplayName("PENDING에서 INDEXED로 바로 전이할 수 없다")
    void PENDING에서_INDEXED로_바로_전이할_수_없다() {
        assertThatThrownBy(() -> pending().succeed(
                        List.of(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8),
                        "fake-embedding-v1",
                        NOW.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    private CareerSearchDocument pending() {
        return CareerSearchDocument.pending(
                DevelopmentUsers.USER_A.userId(),
                CareerExperienceVersionId.newId(),
                "경험명: 프로젝트",
                CONTENT_HASH,
                NOW);
    }
}
