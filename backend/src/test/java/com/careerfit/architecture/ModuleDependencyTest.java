package com.careerfit.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class ModuleDependencyTest {

    private static final JavaClasses APPLICATION_CLASSES =
            new ClassFileImporter().importPackages("com.careerfit");

    @Test
    void identityDoesNotDependOnBusinessOrAiModules() {
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
    void domainModulesDoNotDependOnAnalysisOrApplicationModules() {
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
    void aiDoesNotOwnBusinessDependencies() {
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
    void commonDoesNotContainBusinessDependencies() {
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
