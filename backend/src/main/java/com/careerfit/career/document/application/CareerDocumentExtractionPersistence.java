package com.careerfit.career.document.application;

import com.careerfit.career.document.domain.CareerDocumentAnalysis;
import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.career.document.domain.CareerDocumentPage;
import com.careerfit.identity.UserId;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareerDocumentExtractionPersistence {

    private static final String PAGE_REFERENCE = "database:career_document_page";
    private final CareerDocumentAnalysisRepository repository;
    private final Clock clock;

    public CareerDocumentExtractionPersistence(
            CareerDocumentAnalysisRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public CareerDocumentAnalysis start(UserId userId, CareerDocumentAnalysisId analysisId) {
        CareerDocumentAnalysis analysis = find(userId, analysisId);
        CareerDocumentAnalysis processing = analysis.start(clock.instant());
        repository.save(processing);
        return processing;
    }

    @Transactional
    public void complete(CareerDocumentAnalysis analysis, List<ExtractedPdfPage> extractedPages) {
        List<CareerDocumentPage> pages = extractedPages.stream()
                .map(page -> new CareerDocumentPage(
                        analysis.id(), analysis.documentId(), analysis.userId(), page.pageNumber(),
                        page.text(), page.text().length(), page.checksumSha256()))
                .toList();
        repository.savePages(pages);
        repository.save(analysis.extractionCompleted(PAGE_REFERENCE));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(UserId userId, CareerDocumentAnalysisId analysisId, String failureCode) {
        CareerDocumentAnalysis analysis = find(userId, analysisId);
        repository.save(analysis.fail(failureCode, clock.instant()));
    }

    private CareerDocumentAnalysis find(UserId userId, CareerDocumentAnalysisId analysisId) {
        return repository.find(userId, analysisId)
                .orElseThrow(() -> new IllegalStateException("문서 분석을 찾을 수 없습니다."));
    }
}
