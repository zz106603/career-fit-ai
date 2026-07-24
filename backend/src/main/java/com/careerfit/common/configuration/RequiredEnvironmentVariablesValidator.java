package com.careerfit.common.configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

public final class RequiredEnvironmentVariablesValidator implements EnvironmentPostProcessor {

    static final String DATABASE_PASSWORD = "DB_PASSWORD";

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.acceptsProfiles(Profiles.of("local"))) {
            requireNonBlank(environment, DATABASE_PASSWORD);
        }
    }

    private void requireNonBlank(ConfigurableEnvironment environment, String variableName) {
        String value = environment.getProperty(variableName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "필수 환경변수 " + variableName + "가 설정되지 않았습니다.");
        }
    }
}
