\set ON_ERROR_STOP on
-- Procedimiento manual de demo, NO migración. psql -v correo=cuenta@ejemplo ... -f archivo
-- Solo cuenta existente/activa y base test. No crea contraseña ni imprime credenciales.
BEGIN;
DO $$ BEGIN
    IF current_database()<>'reminishub_test' THEN RAISE EXCEPTION 'Solo reminishub_test'; END IF;
END $$;
SELECT set_config('app.admin_email', :'correo', true);
DO $$
DECLARE filas integer;
BEGIN
    UPDATE public.usuario SET rol='ADMIN'
        WHERE email=lower(btrim(current_setting('app.admin_email'))) AND activo;
    GET DIAGNOSTICS filas=ROW_COUNT;
    IF filas<>1 THEN RAISE EXCEPTION 'Se requiere exactamente una cuenta activa existente'; END IF;
END $$;
COMMIT;
\echo 'Cuenta aprovisionada. Inicia sesión de nuevo para obtener el rol vigente.'
