package com.careerfit.career.document.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.careerfit.identity.CurrentUserProvider;
import com.careerfit.identity.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("경력 문서 서비스 테스트")
class CareerDocumentServiceTest {

    @Test
    @DisplayName("파일 저장 후 DB 저장 실패 시 생성한 파일을 보상 삭제한다")
    void DB_저장_실패_시_생성한_파일을_보상_삭제한다() {
        CareerDocumentRepository repository = mock(CareerDocumentRepository.class);
        FileStoragePort storage = mock(FileStoragePort.class);
        PdfDocumentValidator validator = mock(PdfDocumentValidator.class);
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        UserId userId = new UserId(UUID.randomUUID());
        when(currentUserProvider.currentUserId()).thenReturn(userId);
        when(validator.validate(any())).thenReturn(new ValidatedPdf(1, "a".repeat(64)));
        doThrow(new IllegalStateException("DB 실패")).when(repository).save(any());
        CareerDocumentService service = new CareerDocumentService(
                repository,
                storage,
                validator,
                currentUserProvider,
                Clock.fixed(Instant.parse("2026-07-31T00:00:00Z"), ZoneOffset.UTC));

        assertThatThrownBy(() -> service.upload(
                        new CareerDocumentUpload("../resume.pdf", "application/pdf", new byte[] {1})))
                .isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<String> reference = ArgumentCaptor.forClass(String.class);
        verify(storage).delete(reference.capture());
        org.assertj.core.api.Assertions.assertThat(reference.getValue())
                .startsWith("career-documents/" + userId.value())
                .endsWith(".pdf");
    }
}
