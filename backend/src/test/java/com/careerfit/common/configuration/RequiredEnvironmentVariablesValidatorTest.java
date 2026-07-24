package com.careerfit.common.configuration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

@DisplayName("필수 환경변수 검증 테스트")
class RequiredEnvironmentVariablesValidatorTest {

    private final RequiredEnvironmentVariablesValidator validator =
            new RequiredEnvironmentVariablesValidator();

    @Test
    @DisplayName("local 프로필에서 DB 비밀번호가 누락되면 실패한다")
    void local_프로필에서_DB_비밀번호가_누락되면_실패한다() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("필수 환경변수 DB_PASSWORD가 설정되지 않았습니다.");
    }

    @Test
    @DisplayName("local 프로필에서 DB 비밀번호가 있으면 통과한다")
    void local_프로필에서_DB_비밀번호가_있으면_통과한다() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(RequiredEnvironmentVariablesValidator.DATABASE_PASSWORD, "test-only");
        environment.setActiveProfiles("local");

        assertThatCode(() -> validator.postProcessEnvironment(environment, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("test 프로필은 로컬 DB 비밀번호를 요구하지 않는다")
    void test_프로필은_로컬_DB_비밀번호를_요구하지_않는다() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");

        assertThatCode(() -> validator.postProcessEnvironment(environment, null))
                .doesNotThrowAnyException();
    }
}
