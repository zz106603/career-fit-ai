package com.careerfit.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("영속성 기술 경계 테스트")
class PersistenceTechnologyBoundaryTest {

    private static final JavaClasses APPLICATION_CLASSES =
            new ClassFileImporter().importPackages("com.careerfit");

    @Test
    @DisplayName("JdbcClient는 pgvector 특화 Repository에서만 사용한다")
    void JdbcClient는_pgvector_특화_Repository에서만_사용한다() {
        Set<String> jdbcClientUsers = APPLICATION_CLASSES.stream()
                .filter(javaClass -> !javaClass.getSimpleName().endsWith("Test"))
                .filter(this::dependsOnJdbcClient)
                .map(JavaClass::getName)
                .collect(Collectors.toSet());

        assertThat(jdbcClientUsers)
                .containsExactlyInAnyOrder(
                        "com.careerfit.career.search.infrastructure"
                                + ".JdbcCareerSearchDocumentRepository",
                        "com.careerfit.analysis.search.infrastructure"
                                + ".JdbcCareerCandidateVectorRepository");
    }

    @Test
    @DisplayName("도메인 모델은 JPA API에 의존하지 않는다")
    void 도메인_모델은_JPA_API에_의존하지_않는다() {
        Set<String> violatingDomains = APPLICATION_CLASSES.stream()
                .filter(javaClass -> javaClass.getPackageName().contains(".domain"))
                .filter(javaClass -> javaClass.getDirectDependenciesFromSelf().stream()
                        .anyMatch(dependency -> dependency.getTargetClass()
                                .getPackageName()
                                .startsWith("jakarta.persistence")))
                .map(JavaClass::getName)
                .collect(Collectors.toSet());

        assertThat(violatingDomains).isEmpty();
    }

    private boolean dependsOnJdbcClient(JavaClass javaClass) {
        return javaClass.getDirectDependenciesFromSelf().stream()
                .anyMatch(dependency -> dependency.getTargetClass()
                        .getName()
                        .equals("org.springframework.jdbc.core.simple.JdbcClient"));
    }
}
