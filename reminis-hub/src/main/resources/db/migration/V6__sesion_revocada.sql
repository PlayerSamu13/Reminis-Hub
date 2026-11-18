CREATE TABLE sesion_revocada (
    id uuid PRIMARY KEY,
    eliminar_desde timestamptz NOT NULL
);
CREATE INDEX ix_sesion_revocada_limpieza ON sesion_revocada(eliminar_desde);
