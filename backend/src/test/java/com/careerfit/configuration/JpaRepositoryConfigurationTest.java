package com.careerfit.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:jpa_repository_configuration",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("JPA Repository 구성 테스트")
class JpaRepositoryConfigurationTest {

    @Test
    @DisplayName("Entity 매핑과 Spring Data Repository 쿼리를 구성한다")
    void Entity_매핑과_Spring_Data_Repository_쿼리를_구성한다() {}
}
