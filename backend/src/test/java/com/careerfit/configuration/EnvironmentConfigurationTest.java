package com.careerfit.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

@DisplayName("환경별 설정 로딩 테스트")
class EnvironmentConfigurationTest {

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Test
    @DisplayName("local 프로필은 DB 비밀번호 환경변수를 필수로 요구한다")
    void local_프로필은_DB_비밀번호_환경변수를_필수로_요구한다() throws IOException {
        PropertySource<?> local = loadPropertySource("application-local.yml");
        MutablePropertySources sources = new MutablePropertySources();
        sources.addLast(local);
        PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(sources);

        String configuredPassword = (String) local.getProperty("spring.datasource.password");

        assertThat(configuredPassword).isEqualTo("${DB_PASSWORD}");
        assertThatThrownBy(() -> resolver.resolveRequiredPlaceholders(configuredPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DB_PASSWORD");
    }

    @Test
    @DisplayName("local과 test 프로필은 서로 다른 설정 파일을 사용한다")
    void local과_test_프로필은_서로_다른_설정_파일을_사용한다() throws IOException {
        PropertySource<?> local = loadPropertySource("application-local.yml");
        PropertySource<?> test = loadPropertySource("application-test.yml");

        assertThat(local.getProperty("spring.datasource.url"))
                .isEqualTo("${DB_URL:jdbc:postgresql://localhost:5432/career_fit}");
        assertThat(test.getProperty("spring.datasource.url")).isNull();
        assertThat(local.getProperty("spring.flyway.baseline-version")).isEqualTo(0);
        assertThat(test.getProperty("spring.flyway.baseline-version")).isEqualTo(0);
    }

    private PropertySource<?> loadPropertySource(String resourceName) throws IOException {
        return loader.load(resourceName, new ClassPathResource(resourceName)).getFirst();
    }
}
