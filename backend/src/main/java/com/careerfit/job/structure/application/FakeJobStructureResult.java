package com.careerfit.job.structure.application;

public record FakeJobStructureResult(String requirementText, String sourceExcerpt, String model) {

    public FakeJobStructureResult {
        requirementText = requireText(requirementText);
        sourceExcerpt = requireText(sourceExcerpt);
        model = requireText(model);
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidJobStructureException();
        }
        return value.trim();
    }
}
