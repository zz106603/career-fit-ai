package com.careerfit.career.document.web;

import com.careerfit.career.document.application.CareerDocumentContent;
import com.careerfit.career.document.application.CareerDocumentExtractionService;
import com.careerfit.career.document.application.CareerDocumentService;
import com.careerfit.career.document.application.CareerDocumentUpload;
import com.careerfit.career.document.application.InvalidPdfException;
import com.careerfit.career.document.application.PdfValidationFailure;
import com.careerfit.career.document.domain.CareerDocument;
import com.careerfit.career.document.domain.CareerDocumentId;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/career-documents")
public class CareerDocumentController {

    private final CareerDocumentService service;
    private final CareerDocumentExtractionService extractionService;

    public CareerDocumentController(
            CareerDocumentService service, CareerDocumentExtractionService extractionService) {
        this.service = service;
        this.extractionService = extractionService;
    }

    @PostMapping("/{documentId}/extractions")
    public ResponseEntity<CareerDocumentExtractionResponse> extract(
            @PathVariable UUID documentId) {
        return ResponseEntity.accepted().body(CareerDocumentExtractionResponse.from(
                extractionService.request(new CareerDocumentId(documentId))));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CareerDocumentResponse> upload(
            @RequestPart("file") MultipartFile file) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new InvalidPdfException(
                    PdfValidationFailure.CORRUPTED, "업로드 파일을 읽을 수 없습니다.");
        }
        CareerDocument document = service.upload(new CareerDocumentUpload(
                file.getOriginalFilename(), file.getContentType(), content));
        return ResponseEntity.status(201).body(CareerDocumentResponse.from(document));
    }

    @GetMapping("/{documentId}")
    public CareerDocumentResponse find(@PathVariable UUID documentId) {
        return CareerDocumentResponse.from(
                service.find(new CareerDocumentId(documentId)));
    }

    @GetMapping("/{documentId}/content")
    public ResponseEntity<byte[]> content(@PathVariable UUID documentId) {
        CareerDocumentContent content =
                service.readContent(new CareerDocumentId(documentId));
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(content.document().originalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(content.content().length)
                .body(content.content());
    }
}
