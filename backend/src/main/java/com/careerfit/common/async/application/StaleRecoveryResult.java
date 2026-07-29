package com.careerfit.common.async.application;

public record StaleRecoveryResult(int requeuedCount, int failedCount) {

    public int processedCount() {
        return requeuedCount + failedCount;
    }
}
