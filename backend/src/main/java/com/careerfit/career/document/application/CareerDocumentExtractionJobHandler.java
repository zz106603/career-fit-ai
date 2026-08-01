package com.careerfit.career.document.application;

import com.careerfit.career.document.domain.CareerDocument;
import com.careerfit.career.document.domain.CareerDocumentAnalysis;
import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.common.async.application.JobHandler;
import com.careerfit.common.async.application.JobHandlerException;
import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobType;
import com.careerfit.identity.UserId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CareerDocumentExtractionJobHandler implements JobHandler {

    private final CareerDocumentExtractionPersistence persistence;
    private final CareerDocumentRepository documents;
    private final FileStoragePort fileStorage;
    private final PdfTextExtractor extractor;

    public CareerDocumentExtractionJobHandler(
            CareerDocumentExtractionPersistence persistence,
            CareerDocumentRepository documents,
            FileStoragePort fileStorage,
            PdfTextExtractor extractor) {
        this.persistence = persistence;
        this.documents = documents;
        this.fileStorage = fileStorage;
        this.extractor = extractor;
    }

    @Override
    public JobType type() {
        return JobType.CAREER_DOCUMENT_EXTRACTION;
    }

    @Override
    public void handle(JobExecution execution) {
        UserId userId = new UserId(execution.userId());
        CareerDocumentAnalysisId analysisId = new CareerDocumentAnalysisId(execution.targetId());
        CareerDocumentAnalysis analysis = persistence.start(userId, analysisId);
        try {
            CareerDocument document = documents.findActive(userId, analysis.documentId())
                    .orElseThrow(() -> failure(
                            CareerDocumentExtractionFailure.CAREER_DOCUMENT_NOT_FOUND,
                            "경력 문서를 찾을 수 없습니다."));
            byte[] content = read(document.storageReference());
            List<ExtractedPdfPage> pages = extractor.extract(content);
            if (pages.size() != document.pageCount()) {
                throw failure(CareerDocumentExtractionFailure.PDF_PARSE_FAILED,
                        "PDF 페이지 수가 업로드 정보와 일치하지 않습니다.");
            }
            try {
                persistence.complete(analysis, pages);
            } catch (RuntimeException exception) {
                throw failure(CareerDocumentExtractionFailure.PAGE_TEXT_SAVE_FAILED,
                        "페이지 텍스트 저장에 실패했습니다.");
            }
        } catch (PdfExtractionException exception) {
            fail(analysis, exception.failure());
        } catch (JobHandlerException exception) {
            fail(analysis, CareerDocumentExtractionFailure.valueOf(exception.failureCode()));
        }
    }

    private byte[] read(String storageReference) {
        try {
            return fileStorage.read(storageReference);
        } catch (RuntimeException exception) {
            throw failure(CareerDocumentExtractionFailure.FILE_STORAGE_READ_FAILED,
                    "저장된 PDF를 읽을 수 없습니다.");
        }
    }

    private void fail(CareerDocumentAnalysis analysis, CareerDocumentExtractionFailure failure) {
        persistence.fail(analysis.userId(), analysis.id(), failure.name());
        throw failure(failure, "PDF 텍스트 추출 작업에 실패했습니다.");
    }

    private JobHandlerException failure(
            CareerDocumentExtractionFailure failure, String message) {
        return new JobHandlerException(failure.name(), message);
    }
}
