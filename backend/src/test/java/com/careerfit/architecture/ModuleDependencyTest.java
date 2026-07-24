package com.careerfit.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("모듈 의존성 규칙 테스트")
class ModuleDependencyTest {

    private static final JavaClasses APPLICATION_CLASSES =
            new ClassFileImporter().importPackages("com.careerfit");

    @Test
    @DisplayName("identity는 업무 모듈과 AI 모듈에 의존하지 않는다")
    void identity는_업무_모듈과_AI_모듈에_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.careerfit.identity..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.careerfit.career..",
                        "com.careerfit.job..",
                        "com.careerfit.company..",
                        "com.careerfit.analysis..",
                        "com.careerfit.application..",
                        "com.careerfit.ai..")
                .allowEmptyShould(true);

        rule.check(APPLICATION_CLASSES);
    }

    @Test
    @DisplayName("핵심 도메인 모듈은 조정 모듈에 의존하지 않는다")
    void 핵심_도메인_모듈은_조정_모듈에_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(
                        "com.careerfit.career..",
                        "com.careerfit.job..",
                        "com.careerfit.company..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.careerfit.analysis..",
                        "com.careerfit.application..")
                .allowEmptyShould(true);

        rule.check(APPLICATION_CLASSES);
    }

    @Test
    @DisplayName("AI 모듈은 업무 모듈에 의존하지 않는다")
    void AI_모듈은_업무_모듈에_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.careerfit.ai..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.careerfit.identity..",
                        "com.careerfit.career..",
                        "com.careerfit.job..",
                        "com.careerfit.company..",
                        "com.careerfit.analysis..",
                        "com.careerfit.application..")
                .allowEmptyShould(true);

        rule.check(APPLICATION_CLASSES);
    }

    @Test
    @DisplayName("common은 업무 모듈과 AI 모듈에 의존하지 않는다")
    void common은_업무_모듈과_AI_모듈에_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.careerfit.common..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.careerfit.identity..",
                        "com.careerfit.career..",
                        "com.careerfit.job..",
                        "com.careerfit.company..",
                        "com.careerfit.analysis..",
                        "com.careerfit.application..",
                        "com.careerfit.ai..")
                .allowEmptyShould(true);

        rule.check(APPLICATION_CLASSES);
    }
}
