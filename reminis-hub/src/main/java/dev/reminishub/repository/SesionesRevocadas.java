package dev.reminishub.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Identificadores de sesiones cerradas. Solo conserva metadatos de revocación. */
@Repository
public class SesionesRevocadas {
    private final JdbcTemplate jdbc;

    public SesionesRevocadas(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean contiene(UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM sesion_revocada WHERE id = ?)", Boolean.class, id));
    }

    @Transactional
    public void revocar(UUID id, Instant expiracion) {
        jdbc.update("DELETE FROM sesion_revocada WHERE eliminar_desde < CURRENT_TIMESTAMP");
        // Conserva un margen superior a la tolerancia de reloj del validador.
        jdbc.update("INSERT INTO sesion_revocada(id, eliminar_desde) VALUES (?, ?) ON CONFLICT (id) DO NOTHING",
                id, Timestamp.from(expiracion.plusSeconds(300)));
    }
}
