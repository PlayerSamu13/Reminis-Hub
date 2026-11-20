\set ON_ERROR_STOP on
-- Ejecutar con psql -X -W ... -d reminishub_test -f este_archivo.
-- Crea funciones/triggers y fixtures SOLO en una transacción que termina en ROLLBACK.
DO $$ BEGIN
    IF current_database() <> 'reminishub_test' THEN
        RAISE EXCEPTION 'Solo se permite reminishub_test';
    END IF;
END $$;
SELECT current_database(), current_user;
SELECT version, success FROM public.flyway_schema_history ORDER BY installed_rank;
SELECT rolname, rolsuper, rolcreaterole, rolcreatedb FROM pg_roles WHERE rolname = current_user;
-- Inventario de permisos: no cambia propietarios, roles ni privilegios.
SELECT c.relname, pg_get_userbyid(c.relowner) AS propietario,
    has_table_privilege(current_user,c.oid,'INSERT') AS inserta,
    has_table_privilege(current_user,c.oid,'UPDATE') AS actualiza,
    has_table_privilege(current_user,c.oid,'DELETE') AS elimina,
    has_table_privilege(current_user,c.oid,'TRIGGER') AS crea_trigger
FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
WHERE n.nspname='public' AND c.relname IN ('proyecto','proyecto_autor','bitacora_proyecto');
BEGIN;
\ir V7__integridad_y_auditoria_proyectos.sql

CREATE FUNCTION pg_temp.rechaza(sql text, mensaje text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    BEGIN
        EXECUTE sql;
    EXCEPTION WHEN check_violation THEN
        IF SQLERRM <> mensaje THEN
            RAISE EXCEPTION 'FAIL: rechazo distinto del esperado (%), recibido (%)', mensaje, SQLERRM;
        END IF;
        RETURN;
    END;
    RAISE EXCEPTION 'FAIL: se acepto una operacion prohibida';
END $$;

DO $$
DECLARE
    colaborador bigint;
    administrador bigint;
    proyecto_prueba bigint;
    cuenta_historial integer;
    identificador text := replace(gen_random_uuid()::text, '-', '');
BEGIN
    INSERT INTO public.usuario(email,nombre,rol) VALUES
        (identificador || '@example.invalid','Colaborador de prueba','COLABORADOR') RETURNING id INTO colaborador;
    INSERT INTO public.usuario(email,nombre,rol) VALUES
        ('admin-' || identificador || '@example.invalid','Administrador de prueba','ADMIN') RETURNING id INTO administrador;
    PERFORM set_config('app.actor_user_id', colaborador::text, true);
    INSERT INTO public.proyecto(nombre,resumen,descripcion,url_repositorio,anio,semestre,
        categoria_id,cargado_por,terminos_aceptados_en,terminos_version)
    VALUES ('Proyecto de prueba',btrim(repeat('Resumen de prueba. ',3)),btrim(repeat('Descripcion de prueba. ',3)),
        'https://github.com/prueba/' || identificador,2026,2,
        (SELECT id FROM public.categoria ORDER BY id LIMIT 1),colaborador,now(),'v1')
    RETURNING id INTO proyecto_prueba;
    SELECT count(*) INTO cuenta_historial FROM public.bitacora_proyecto
        WHERE proyecto_id = proyecto_prueba AND operacion = 'INSERT' AND usuario_aplicacion_id = colaborador;
    IF cuenta_historial <> 1 THEN RAISE EXCEPTION 'FAIL: actor de alta'; END IF;

    PERFORM pg_temp.rechaza(format(
        'INSERT INTO public.bitacora_proyecto(proyecto_id,operacion,estado_nuevo,rol_bd_sesion) VALUES (%s,''INSERT'',''PENDIENTE_REVISION'',session_user)',
        proyecto_prueba), 'Bitacora solo desde trigger de proyecto');
    PERFORM pg_temp.rechaza(format(
        'INSERT INTO public.proyecto_autor(proyecto_id,nombre_publico,orden,medio_autorizacion) VALUES (%s,''Autor ficticio'',1,''DATO_FICTICIO'')',
        proyecto_prueba), 'Autor ficticio requiere proyecto ficticio');
    PERFORM pg_temp.rechaza(format(
        'INSERT INTO public.proyecto_autor(proyecto_id,usuario_id,nombre_publico,orden,medio_autorizacion,autorizado_en,version_autorizacion) VALUES (%s,%s,''Autor propio'',1,''CUENTA_PROPIA'',now(),'''')',
        proyecto_prueba,colaborador), 'Version de autorizacion requerida');
    UPDATE public.usuario SET activo=false WHERE id=colaborador;
    PERFORM pg_temp.rechaza(format('UPDATE public.proyecto SET nombre=''Otro nombre'' WHERE id=%s',proyecto_prueba), 'Actor no disponible');
    UPDATE public.usuario SET activo=true,actualizado_en=TIMESTAMPTZ '2000-01-01' WHERE id=colaborador;
    IF (SELECT actualizado_en FROM public.usuario WHERE id=colaborador) <= TIMESTAMPTZ '2000-01-01' THEN
        RAISE EXCEPTION 'FAIL: fecha de actualizacion usuario';
    END IF;

    PERFORM pg_temp.rechaza(format('UPDATE public.proyecto SET estado=''PUBLICADO'' WHERE id=%s',proyecto_prueba), 'Moderacion requiere administrador');
    PERFORM pg_temp.rechaza(format('UPDATE public.proyecto SET es_ficticio=true WHERE id=%s',proyecto_prueba), 'Metadatos inmutables');
    PERFORM set_config('app.actor_user_id','',true);
    PERFORM pg_temp.rechaza(format('UPDATE public.proyecto SET nombre=''Otro nombre'' WHERE id=%s',proyecto_prueba), 'Actor transaccional requerido');
    PERFORM set_config('app.actor_user_id',administrador::text,true);
    UPDATE public.proyecto SET estado='RECHAZADO',motivo_decision='Falta informacion' WHERE id=proyecto_prueba;
    PERFORM set_config('app.actor_user_id',colaborador::text,true);
    UPDATE public.proyecto SET estado='PENDIENTE_REVISION',motivo_decision=NULL WHERE id=proyecto_prueba;
    PERFORM set_config('app.actor_user_id',administrador::text,true);
    UPDATE public.proyecto SET estado='PUBLICADO' WHERE id=proyecto_prueba;
    PERFORM pg_temp.rechaza(format('UPDATE public.proyecto SET nombre=''Nombre alterado'' WHERE id=%s',proyecto_prueba), 'Edicion no permitida');
    UPDATE public.proyecto SET estado='RETIRADO' WHERE id=proyecto_prueba;
    PERFORM pg_temp.rechaza(format('UPDATE public.proyecto SET estado=''PENDIENTE_REVISION'' WHERE id=%s',proyecto_prueba), 'Retirado es final');
    PERFORM pg_temp.rechaza(format('DELETE FROM public.proyecto WHERE id=%s',proyecto_prueba), 'Use retiro logico');
    PERFORM pg_temp.rechaza(format('DELETE FROM public.bitacora_proyecto WHERE proyecto_id=%s',proyecto_prueba), 'La bitacora es de solo insercion');
    SELECT count(*) INTO cuenta_historial FROM public.bitacora_proyecto WHERE proyecto_id=proyecto_prueba;
    IF cuenta_historial <> 5 THEN RAISE EXCEPTION 'FAIL: historial de transiciones'; END IF;
    RAISE NOTICE 'PASS: alta, actor, transiciones, rechazo de actor ausente, inmutabilidad y retiro final';
END $$;
ROLLBACK;
DO $$ BEGIN
    IF nullif(current_setting('app.actor_user_id',true),'') IS NOT NULL THEN
        RAISE EXCEPTION 'FAIL: actor persiste tras rollback';
    END IF;
END $$;
SELECT 'PASS: rollback de la migracion candidata y contexto local en la misma conexion' AS resultado;
