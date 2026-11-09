package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración de migraciones Flyway sobre una base de datos real PostgreSQL 16.
 * Valida la transición completa desde V1 (esquema inicial UUID) hasta V2 (esquema oficial BIGINT).
 */
@Testcontainers(disabledWithoutDocker = true)
class FlywayPostgreSqlMigrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("styleradar_donations_test")
            .withUsername("test_user")
            .withPassword("test_password");

    @Test
    @DisplayName("Debe ejecutar exitosamente las migraciones Flyway V1 y V2 en PostgreSQL 16")
    void shouldApplyFlywayMigrationsCleanlyOnPostgreSql() throws SQLException {
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
