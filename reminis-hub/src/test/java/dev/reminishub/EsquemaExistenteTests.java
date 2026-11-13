package dev.reminishub;

import dev.reminishub.model.enums.MedioAutorizacion;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/** Comprueba el historial y V3 sin migrar las tablas existentes. */
@EnabledIfEnvironmentVariable(named = "E1A_DB_CHECKS", matches = "true")
class EsquemaExistenteTests {

    @Test
    void migracionesAplicadasConservanSusChecksums() {
        Flyway.configure()
                .dataSource(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"))
                .locations("classpath:db/migration")
                // V3 puede estar pendiente: validate no debe aplicarla.
                .ignoreMigrationPatterns("*:pending")
                .load().validate();
    }

    @Test
    void autorizacionExigeCuentaYConservaElNombreDelEnum() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"))) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                // LIKE copia CHECK e identidad, pero no FK. Solo se prueba el contrato
                // de autorizacion; no se afirma haber probado integridad referencial.
                statement.execute("CREATE TEMP TABLE proyecto_autor "
                        + "(LIKE public.proyecto_autor INCLUDING ALL) ON COMMIT DROP");
                var resource = getClass().getResourceAsStream(
                        "/db/migration/V3__vinculo_autor_cuenta_propia.sql");
                assertNotNull(resource);
                String migration;
                try (resource) {
                    migration = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
                }
                // pg_temp precede a public: ALTER afecta unicamente la copia temporal.
                statement.execute("SET LOCAL search_path TO pg_temp, public");
                statement.execute("ALTER TABLE pg_temp.proyecto_autor "
                        + "DROP CONSTRAINT IF EXISTS ck_proyecto_autor_cuenta_propia");
                statement.execute(migration);

                try (var insert = connection.prepareStatement("""
                        INSERT INTO pg_temp.proyecto_autor
                            (proyecto_id, nombre_publico, orden, medio_autorizacion)
                        VALUES (1, 'Autor ficticio', 1, ?) RETURNING medio_autorizacion
                        """)) {
                    insert.setString(1, MedioAutorizacion.DATO_FICTICIO.name());
                    try (var result = insert.executeQuery()) {
                        assertTrue(result.next());
                        assertEquals(MedioAutorizacion.DATO_FICTICIO,
                                MedioAutorizacion.valueOf(result.getString(1)));
                    }
                }

                statement.executeUpdate("""
                        INSERT INTO pg_temp.proyecto_autor
                            (proyecto_id, usuario_id, nombre_publico, orden,
                             medio_autorizacion, autorizado_en, version_autorizacion)
                        VALUES (1, 123, 'Autor de prueba', 2, 'CUENTA_PROPIA', now(), 'v1')
                        """);
                var beforeInvalid = connection.setSavepoint();
                SQLException error = assertThrows(SQLException.class, () -> statement.executeUpdate("""
                        INSERT INTO pg_temp.proyecto_autor
                            (proyecto_id, nombre_publico, orden,
                             medio_autorizacion, autorizado_en, version_autorizacion)
                        VALUES (1, 'Autor sin cuenta', 3, 'CUENTA_PROPIA', now(), 'v1')
                        """));
                assertEquals("23514", error.getSQLState());
                connection.rollback(beforeInvalid);
            } finally {
                connection.rollback();
            }
        }
    }
}
