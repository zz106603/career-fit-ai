package com.careerfit.career.document.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PDF 페이지 텍스트 추출 테스트")
class PdfTextExtractorTest {

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    @Test
    @DisplayName("페이지 순서와 빈 페이지를 보존해 텍스트를 추출한다")
    void 페이지_순서와_빈_페이지를_보존한다() throws Exception {
        List<ExtractedPdfPage> pages = extractor.extract(pdf("first", null, "third"));

        assertThat(pages).extracting(ExtractedPdfPage::pageNumber).containsExactly(1, 2, 3);
        assertThat(pages.get(0).text()).contains("first");
        assertThat(pages.get(1).text()).isEmpty();
        assertThat(pages.get(2).text()).contains("third");
        assertThat(pages).allSatisfy(page -> assertThat(page.checksumSha256())
                .matches("[0-9a-f]{64}"));
    }

    @Test
    @DisplayName("모든 페이지가 비어 있으면 명시적인 실패 코드로 거절한다")
    void 모든_페이지가_비어_있으면_거절한다() throws Exception {
        assertThatThrownBy(() -> extractor.extract(pdf(null, null)))
                .isInstanceOf(PdfExtractionException.class)
                .extracting("failure")
                .isEqualTo(CareerDocumentExtractionFailure.PDF_TEXT_EMPTY);
    }

    @Test
    @DisplayName("손상된 PDF는 파싱 실패로 구분한다")
    void 손상된_PDF는_파싱_실패로_구분한다() {
        assertThatThrownBy(() -> extractor.extract("invalid".getBytes()))
                .isInstanceOf(PdfExtractionException.class)
                .extracting("failure")
                .isEqualTo(CareerDocumentExtractionFailure.PDF_PARSE_FAILED);
    }

    private byte[] pdf(String... pageTexts) throws Exception {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (String pageText : pageTexts) {
                PDPage page = new PDPage();
                document.addPage(page);
                if (pageText != null) {
                    try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                        stream.beginText();
                        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        stream.newLineAtOffset(50, 700);
                        stream.showText(pageText);
                        stream.endText();
                    }
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
