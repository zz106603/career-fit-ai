package com.careerfit;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("데이터베이스 Migration 통합 테스트")
class DatabaseMigrationIntegrationTest extends PostgresIntegrationTest {

    @Test
    @DisplayName("Spring 애플리케이션 컨텍스트를 로드한다")
    void 스프링_애플리케이션_컨텍스트를_로드한다() {
    }

    @Test
    @DisplayName("빈 DB에 Migration을 적용하고 재실행해도 변경되지 않는다")
    void 빈_DB에_Migration을_적용하고_재실행해도_변경되지_않는다() throws Exception {
        String schema = "empty_database";
        createSchema(schema, false);

        Flyway flyway = flyway(schema);
        MigrateResult firstMigration = flyway.migrate();
        MigrateResult secondMigration = flyway.migrate();

        assertThat(firstMigration.migrationsExecuted).isEqualTo(1);
        assertThat(secondMigration.migrationsExecuted).isZero();
        assertVectorTypeCanBeUsed();
    }

    @Test
    @DisplayName("기존 DB를 baseline한 뒤 초기 Migration을 적용한다")
    void 기존_DB를_baseline한_뒤_초기_Migration을_적용한다() throws Exception {
        String schema = "existing_database";
        createSchema(schema, true);

        MigrateResult migration = flyway(schema).migrate();

        assertThat(migration.migrationsExecuted).isEqualTo(1);
        assertThat(tableExists(schema, "legacy_marker")).isTrue();
        assertVectorTypeCanBeUsed();
    }

    private Flyway flyway(String schema) {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .defaultSchema(schema)
                .schemas(schema)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
    }

    private void createSchema(String schema, boolean withExistingTable) throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            if (withExistingTable) {
                statement.execute("CREATE TABLE " + schema + ".legacy_marker (id bigint PRIMARY KEY)");
            }
        }
    }

    private boolean tableExists(String schema, String table) throws Exception {
        try (Connection connection = connection(); ResultSet result = connection.getMetaData()
                .getTables(null, schema, table, new String[] {"TABLE"})) {
            return result.next();
        }
    }

    private void assertVectorTypeCanBeUsed() throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TEMP TABLE vector_probe (embedding vector(3))");
            statement.execute("INSERT INTO vector_probe VALUES ('[1,2,3]')");
            try (ResultSet result = statement.executeQuery("SELECT embedding::text FROM vector_probe")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("[1,2,3]");
            }
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
