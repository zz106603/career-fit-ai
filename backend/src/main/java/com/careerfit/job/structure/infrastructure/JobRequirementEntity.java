package com.careerfit.job.structure.infrastructure;

import com.careerfit.job.structure.domain.JobRequirementCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "job_requirement")
class JobRequirementEntity {

    @Id
    @Column(name = "requirement_id", nullable = false)
    private UUID id;

    @Column(name = "job_posting_analysis_id", nullable = false)
    private UUID analysisId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private JobRequirementCategory category;

    @Column(name = "requirement_text", nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "source_excerpt", nullable = false, columnDefinition = "text")
    private String sourceExcerpt;

    @Column(name = "sequence_no", nullable = false)
    private int sequence;

    protected JobRequirementEntity() {}

    JobRequirementEntity(
            UUID id,
            UUID analysisId,
            UUID userId,
            JobRequirementCategory category,
            String text,
            String sourceExcerpt,
            int sequence) {
        this.id = id;
        this.analysisId = analysisId;
        this.userId = userId;
        this.category = category;
        this.text = text;
        this.sourceExcerpt = sourceExcerpt;
        this.sequence = sequence;
    }

    UUID id() { return id; }
    UUID analysisId() { return analysisId; }
    JobRequirementCategory category() { return category; }
    String text() { return text; }
    String sourceExcerpt() { return sourceExcerpt; }
    int sequence() { return sequence; }
}
