package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración de migraciones Flyway sobre una base de datos real PostgreSQL 16.
 * Valida la transición completa desde V1 (esquema inicial UUID) hasta V2 (esquema oficial BIGINT).
 */
class FlywayPostgreSqlMigrationTest {

    static PostgreSQLContainer<?> postgres;

    @BeforeAll
    static void setUp() {
        boolean isCi = "true".equalsIgnoreCase(System.getenv("CI"))
                || "true".equalsIgnoreCase(System.getenv("GITHUB_ACTIONS"));
        boolean dockerAvailable = DockerClientFactory.instance().isDockerAvailable();

        if (!isCi && !dockerAvailable) {
            Assumptions.abort("Docker no está disponible en el entorno local. Se omite la prueba de integración de PostgreSQL.");
            return;
        }

        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("styleradar_donations_test")
                .withUsername("test_user")
                .withPassword("test_password");
        postgres.start();
    }

    @AfterAll
    static void tearDown() {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
        }
    }

    @Test
    @DisplayName("Debe ejecutar exitosamente las migraciones Flyway V1 y V2 en PostgreSQL 16")
    void shouldApplyFlywayMigrationsCleanlyOnPostgreSql() throws SQLException {
        assertThat(postgres).isNotNull();
        assertThat(postgres.isRunning()).isTrue();

        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        int appliedMigrations = flyway.migrate().migrationsExecuted;
        assertThat(appliedMigrations).isGreaterThanOrEqualTo(2);

        MigrationInfo[] info = flyway.info().all();
        assertThat(info).hasSizeGreaterThanOrEqualTo(2);
        assertThat(info[0].getVersion().getVersion()).isEqualTo("1");
        assertThat(info[0].getState().isApplied()).isTrue();
        assertThat(info[1].getVersion().getVersion()).isEqualTo("2");
        assertThat(info[1].getState().isApplied()).isTrue();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            DatabaseMetaData metaData = conn.getMetaData();

            // Verificar que las tablas oficiales de V2 existen
            assertThat(tableExists(metaData, "publicaciones_segunda_mano")).isTrue();
            assertThat(tableExists(metaData, "fotos_publicacion")).isTrue();
            assertThat(tableExists(metaData, "solicitudes_donacion")).isTrue();

            // Verificar que las tablas temporales de V1 fueron eliminadas en la transición
            assertThat(tableExists(metaData, "articulos_segunda_mano")).isFalse();
            assertThat(tableExists(metaData, "citas_recogida")).isFalse();
        }
    }

    private boolean tableExists(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (ResultSet rs = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }
}
