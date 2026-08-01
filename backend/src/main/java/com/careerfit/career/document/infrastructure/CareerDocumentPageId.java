package com.careerfit.career.document.infrastructure;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

class CareerDocumentPageId implements Serializable {
    private UUID analysisId;
    private int pageNumber;
    protected CareerDocumentPageId() {}
    CareerDocumentPageId(UUID analysisId, int pageNumber) { this.analysisId=analysisId; this.pageNumber=pageNumber; }
    @Override public boolean equals(Object other) {
        return this == other || other instanceof CareerDocumentPageId id
                && pageNumber == id.pageNumber && Objects.equals(analysisId, id.analysisId);
    }
    @Override public int hashCode() { return Objects.hash(analysisId, pageNumber); }
}
