package com.careerfit.career.document.infrastructure;

import com.careerfit.career.document.application.CareerDocumentAnalysisRepository;
import com.careerfit.career.document.domain.*;
import com.careerfit.common.async.domain.JobExecutionId;
import com.careerfit.identity.UserId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaCareerDocumentAnalysisRepository implements CareerDocumentAnalysisRepository {
    private static final List<CareerDocumentAnalysisStatus> ACTIVE = List.of(
            CareerDocumentAnalysisStatus.QUEUED, CareerDocumentAnalysisStatus.PROCESSING);
    private final SpringDataCareerDocumentAnalysisRepository analyses;
    private final SpringDataCareerDocumentPageRepository pages;
    public JpaCareerDocumentAnalysisRepository(SpringDataCareerDocumentAnalysisRepository analyses,
            SpringDataCareerDocumentPageRepository pages) { this.analyses=analyses; this.pages=pages; }

    @Override @Transactional
    public void save(CareerDocumentAnalysis analysis) { analyses.saveAndFlush(toEntity(analysis)); }
    @Override @Transactional(readOnly=true)
    public Optional<CareerDocumentAnalysis> find(UserId userId, CareerDocumentAnalysisId id) {
        return analyses.findByIdAndUserId(id.value(), userId.value()).map(this::toDomain);
    }
    @Override @Transactional(readOnly=true)
    public Optional<CareerDocumentAnalysis> findActive(UserId userId, CareerDocumentId documentId, String inputVersion) {
        return analyses.findFirstByUserIdAndDocumentIdAndInputVersionAndStatusIn(
                userId.value(), documentId.value(), inputVersion, ACTIVE).map(this::toDomain);
    }
    @Override @Transactional
    public void savePages(List<CareerDocumentPage> values) {
        pages.saveAllAndFlush(values.stream().map(this::toEntity).toList());
    }
    @Override @Transactional(readOnly=true)
    public List<CareerDocumentPage> findPages(UserId userId, CareerDocumentAnalysisId id) {
        return pages.findByAnalysisIdAndUserIdOrderByPageNumber(id.value(), userId.value())
                .stream().map(this::toDomain).toList();
    }
    private CareerDocumentAnalysisEntity toEntity(CareerDocumentAnalysis a) {
        return new CareerDocumentAnalysisEntity(a.id().value(), a.documentId().value(), a.userId().value(),
                a.jobExecutionId().value(), a.inputKind(), a.status(), a.inputVersion(), a.workflowVersion(),
                a.extractedTextReference(), a.failureCode(), a.createdAt(), a.startedAt(), a.completedAt());
    }
    private CareerDocumentAnalysis toDomain(CareerDocumentAnalysisEntity e) {
        return new CareerDocumentAnalysis(new CareerDocumentAnalysisId(e.id()), new CareerDocumentId(e.documentId()),
                new UserId(e.userId()), new JobExecutionId(e.jobExecutionId()), e.inputKind(), e.status(),
                e.inputVersion(), e.workflowVersion(), e.extractedTextReference(), e.failureCode(),
                e.createdAt(), e.startedAt(), e.completedAt());
    }
    private CareerDocumentPageEntity toEntity(CareerDocumentPage p) {
        return new CareerDocumentPageEntity(p.analysisId().value(), p.documentId().value(), p.userId().value(),
                p.pageNumber(), p.text(), p.textLength(), p.checksumSha256());
    }
    private CareerDocumentPage toDomain(CareerDocumentPageEntity e) {
        return new CareerDocumentPage(new CareerDocumentAnalysisId(e.analysisId()), new CareerDocumentId(e.documentId()),
                new UserId(e.userId()), e.pageNumber(), e.text(), e.textLength(), e.checksumSha256());
    }
}
