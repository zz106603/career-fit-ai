package com.careerfit.architecture.fixture.identity;

import com.careerfit.architecture.fixture.ai.ForbiddenAiDependency;

public final class IdentityWithForbiddenDependency {

    private final ForbiddenAiDependency forbiddenAiDependency;

    public IdentityWithForbiddenDependency(ForbiddenAiDependency forbiddenAiDependency) {
        this.forbiddenAiDependency = forbiddenAiDependency;
    }

    public ForbiddenAiDependency forbiddenAiDependency() {
        return forbiddenAiDependency;
    }
}
