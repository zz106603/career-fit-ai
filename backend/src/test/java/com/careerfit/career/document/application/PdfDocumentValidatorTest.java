package com.careerfit.career.document.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PDF 문서 검증기 테스트")
class PdfDocumentValidatorTest {

    private final PdfDocumentValidator validator = new PdfDocumentValidator();

    @Test
    @DisplayName("정상 PDF의 페이지 수와 체크섬을 반환한다")
    void 정상_PDF의_페이지_수와_체크섬을_반환한다() throws IOException {
        byte[] pdf = pdf(2, false);

        ValidatedPdf validated =
                validator.validate(new CareerDocumentUpload("resume.pdf", "application/pdf", pdf));

        assertThat(validated.pageCount()).isEqualTo(2);
        assertThat(validated.checksumSha256()).hasSize(64);
    }

    @Test
    @DisplayName("빈 파일과 MIME 불일치와 시그니처 위장을 거절한다")
    void 빈_파일과_MIME_불일치와_시그니처_위장을_거절한다() throws IOException {
        assertFailure(new byte[0], "application/pdf", PdfValidationFailure.EMPTY);
        assertFailure(pdf(1, false), "text/plain", PdfValidationFailure.CONTENT_TYPE);
        assertFailure("not-pdf".getBytes(), "application/pdf", PdfValidationFailure.SIGNATURE);
    }

    @Test
    @DisplayName("10 MiB 초과 파일과 50페이지 초과 PDF를 거절한다")
    void 크기와_페이지_상한을_초과한_PDF를_거절한다() throws IOException {
        byte[] oversized = new byte[(int) PdfDocumentValidator.MAX_BYTES + 1];
        System.arraycopy("%PDF-".getBytes(), 0, oversized, 0, 5);

        assertFailure(oversized, "application/pdf", PdfValidationFailure.TOO_LARGE);
        assertFailure(pdf(51, false), "application/pdf", PdfValidationFailure.PAGE_COUNT);
    }

    @Test
    @DisplayName("손상되거나 암호화된 PDF를 거절한다")
    void 손상되거나_암호화된_PDF를_거절한다() throws IOException {
        assertFailure("%PDF-broken".getBytes(), "application/pdf", PdfValidationFailure.CORRUPTED);
        assertFailure(pdf(1, true), "application/pdf", PdfValidationFailure.ENCRYPTED);
    }

    private void assertFailure(
            byte[] content, String contentType, PdfValidationFailure failure) {
        assertThatThrownBy(() -> validator.validate(
                        new CareerDocumentUpload("resume.pdf", contentType, content)))
                .isInstanceOfSatisfying(
                        InvalidPdfException.class,
                        exception -> assertThat(exception.failure()).isEqualTo(failure));
    }

    private byte[] pdf(int pages, boolean encrypted) throws IOException {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int index = 0; index < pages; index++) {
                document.addPage(new PDPage());
            }
            if (encrypted) {
                StandardProtectionPolicy policy = new StandardProtectionPolicy(
                        "owner-secret", "user-secret", new AccessPermission());
                policy.setEncryptionKeyLength(128);
                document.protect(policy);
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
