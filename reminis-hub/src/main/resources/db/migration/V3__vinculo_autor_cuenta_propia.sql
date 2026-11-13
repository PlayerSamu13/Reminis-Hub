-- V1 y V2 ya estan aplicadas: esta correccion conserva su historial.
-- Si existen autores CUENTA_PROPIA sin usuario, la migracion falla para
-- que se revise su autorizacion real; no inventa vinculos ni borra registros.
ALTER TABLE proyecto_autor
    ADD CONSTRAINT ck_proyecto_autor_cuenta_propia
    CHECK (medio_autorizacion <> 'CUENTA_PROPIA' OR usuario_id IS NOT NULL);
