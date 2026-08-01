package com.careerfit.career.document.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfTextExtractor {

    public List<ExtractedPdfPage> extract(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            if (document.isEncrypted()) {
                throw failure(CareerDocumentExtractionFailure.PDF_ENCRYPTED, "암호화된 PDF입니다.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            List<ExtractedPdfPage> pages = new ArrayList<>();
            boolean hasText = false;
            for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                String text = normalizeLineBreaks(stripper.getText(document));
                hasText |= !text.isBlank();
                pages.add(new ExtractedPdfPage(pageNumber, text, checksum(text)));
            }
            if (!hasText) {
                throw failure(CareerDocumentExtractionFailure.PDF_TEXT_EMPTY, "추출할 텍스트가 없습니다.");
            }
            return List.copyOf(pages);
        } catch (PdfExtractionException exception) {
            throw exception;
        } catch (IOException exception) {
            String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
            CareerDocumentExtractionFailure failure = message.contains("password")
                    ? CareerDocumentExtractionFailure.PDF_ENCRYPTED
                    : CareerDocumentExtractionFailure.PDF_PARSE_FAILED;
            throw failure(failure, "PDF 텍스트 추출에 실패했습니다.");
        }
    }

    private String normalizeLineBreaks(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private String checksum(String text) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private PdfExtractionException failure(CareerDocumentExtractionFailure failure, String message) {
        return new PdfExtractionException(failure, message);
    }
}
