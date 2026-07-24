package com.careerfit.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.architecture.fixture.ai.ForbiddenAiDependency;
import com.careerfit.architecture.fixture.identity.IdentityWithForbiddenDependency;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("금지 모듈 의존 fixture 테스트")
class ModuleDependencyViolationFixtureTest {

    @Test
    @DisplayName("identity가 AI에 의존하는 fixture를 모듈 규칙이 거부한다")
    void identity가_AI에_의존하는_fixture를_모듈_규칙이_거부한다() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..fixture.identity..")
                .should().dependOnClassesThat().resideInAPackage("..fixture.ai..");

        assertThatThrownBy(() -> rule.check(new ClassFileImporter().importClasses(
                        IdentityWithForbiddenDependency.class,
                        ForbiddenAiDependency.class)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Architecture Violation");
    }
}
