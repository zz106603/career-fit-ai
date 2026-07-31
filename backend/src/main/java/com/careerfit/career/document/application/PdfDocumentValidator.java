package com.careerfit.career.document.application;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.stereotype.Component;

@Component
public class PdfDocumentValidator {

    static final long MAX_BYTES = 10L * 1024 * 1024;
    static final int MAX_PAGES = 50;
    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};

    public ValidatedPdf validate(CareerDocumentUpload upload) {
        byte[] content = upload.content();
        if (content.length == 0) {
            throw invalid(PdfValidationFailure.EMPTY, "빈 PDF는 업로드할 수 없습니다.");
        }
        if (!"application/pdf".equalsIgnoreCase(normalizeContentType(upload.contentType()))) {
            throw invalid(PdfValidationFailure.CONTENT_TYPE, "PDF 콘텐츠 유형만 허용합니다.");
        }
        if (content.length > MAX_BYTES) {
            throw invalid(PdfValidationFailure.TOO_LARGE, "PDF는 10 MiB 이하여야 합니다.");
        }
        if (!hasPdfSignature(content)) {
            throw invalid(PdfValidationFailure.SIGNATURE, "PDF 파일 시그니처가 올바르지 않습니다.");
        }

        try (PDDocument document = Loader.loadPDF(content)) {
            if (document.isEncrypted()) {
                throw invalid(PdfValidationFailure.ENCRYPTED, "암호화된 PDF는 지원하지 않습니다.");
            }
            int pageCount = document.getNumberOfPages();
            if (pageCount < 1 || pageCount > MAX_PAGES) {
                throw invalid(PdfValidationFailure.PAGE_COUNT, "PDF 페이지 수는 1~50이어야 합니다.");
            }
            return new ValidatedPdf(pageCount, sha256(content));
        } catch (InvalidPasswordException exception) {
            throw invalid(PdfValidationFailure.ENCRYPTED, "암호화된 PDF는 지원하지 않습니다.");
        } catch (IOException exception) {
            throw invalid(PdfValidationFailure.CORRUPTED, "손상된 PDF는 업로드할 수 없습니다.");
        }
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int parameterStart = contentType.indexOf(';');
        return (parameterStart < 0 ? contentType : contentType.substring(0, parameterStart)).trim();
    }

    private static boolean hasPdfSignature(byte[] content) {
        if (content.length < PDF_SIGNATURE.length) {
            return false;
        }
        for (int index = 0; index < PDF_SIGNATURE.length; index++) {
            if (content[index] != PDF_SIGNATURE[index]) {
                return false;
            }
        }
        return true;
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private static InvalidPdfException invalid(
            PdfValidationFailure failure, String message) {
        return new InvalidPdfException(failure, message);
    }
}
