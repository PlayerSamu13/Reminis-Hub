-- S2: versión revisada con el verificador transaccional sobre reminishub_test.
-- No reconstruye historia anterior ni modifica V1-V6 o datos existentes.
-- Los roles propietarios/superusuarios pueden desactivar triggers: no son una frontera
-- contra el administrador de BD. El rol de ejecución no debe ser propietario en producción.

DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM public.proyecto_autor a JOIN public.proyecto p ON p.id=a.proyecto_id
        WHERE (a.medio_autorizacion='DATO_FICTICIO' AND NOT p.es_ficticio)
           OR (a.medio_autorizacion<>'DATO_FICTICIO' AND nullif(btrim(a.version_autorizacion),'') IS NULL)) THEN
        RAISE EXCEPTION 'Revisar autorizaciones existentes antes de V7; no se modificaron datos';
    END IF;
END $$;

CREATE FUNCTION public.rh_proteger_proyecto() RETURNS trigger
LANGUAGE plpgsql SET search_path = pg_catalog, public AS $$
DECLARE
    actor_texto text := nullif(current_setting('app.actor_user_id', true), '');
    actor bigint;
    rol_actor text;
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Use retiro logico' USING ERRCODE = '23514';
    END IF;
    IF actor_texto IS NULL OR actor_texto !~ '^[0-9]{1,18}$' THEN
        RAISE EXCEPTION 'Actor transaccional requerido' USING ERRCODE = '23514';
    END IF;
    actor := actor_texto::bigint;
    SELECT rol INTO rol_actor FROM public.usuario WHERE id = actor AND activo;
    IF rol_actor IS NULL THEN
        RAISE EXCEPTION 'Actor no disponible' USING ERRCODE = '23514';
    END IF;
    IF TG_OP = 'INSERT' THEN
        IF NEW.estado <> 'PENDIENTE_REVISION' OR NEW.cargado_por <> actor OR NEW.es_ficticio THEN
            RAISE EXCEPTION 'Alta de proyecto no valida' USING ERRCODE = '23514';
        END IF;
    ELSE
        IF NEW.es_ficticio IS DISTINCT FROM OLD.es_ficticio
            OR NEW.cargado_por IS DISTINCT FROM OLD.cargado_por
            OR NEW.creado_en IS DISTINCT FROM OLD.creado_en THEN
            RAISE EXCEPTION 'Metadatos inmutables' USING ERRCODE = '23514';
        END IF;
        IF OLD.estado = 'RETIRADO' THEN
            RAISE EXCEPTION 'Retirado es final' USING ERRCODE = '23514';
        END IF;
        IF (to_jsonb(NEW) - ARRAY['estado','motivo_decision','version','actualizado_en'])
            IS DISTINCT FROM (to_jsonb(OLD) - ARRAY['estado','motivo_decision','version','actualizado_en'])
            AND (OLD.estado = 'PUBLICADO' OR actor <> OLD.cargado_por) THEN
            RAISE EXCEPTION 'Edicion no permitida' USING ERRCODE = '23514';
        END IF;
        IF NEW.estado IS DISTINCT FROM OLD.estado THEN
            IF NOT ((OLD.estado = 'PENDIENTE_REVISION' AND NEW.estado IN ('PUBLICADO','RECHAZADO','RETIRADO'))
                OR (OLD.estado = 'RECHAZADO' AND NEW.estado IN ('PENDIENTE_REVISION','RETIRADO'))
                OR (OLD.estado = 'PUBLICADO' AND NEW.estado = 'RETIRADO')) THEN
                RAISE EXCEPTION 'Transicion no valida' USING ERRCODE = '23514';
            END IF;
            IF NEW.estado IN ('PUBLICADO','RECHAZADO') AND rol_actor <> 'ADMIN' THEN
                RAISE EXCEPTION 'Moderacion requiere administrador' USING ERRCODE = '23514';
            END IF;
        END IF;
        IF actor <> OLD.cargado_por AND rol_actor <> 'ADMIN' THEN
            RAISE EXCEPTION 'Proyecto ajeno' USING ERRCODE = '23514';
        END IF;
    END IF;
    NEW.actualizado_en := clock_timestamp();
    RETURN NEW;
END;
$$;

CREATE TRIGGER rh_proyecto_integridad BEFORE INSERT OR UPDATE OR DELETE ON public.proyecto
FOR EACH ROW EXECUTE FUNCTION public.rh_proteger_proyecto();

CREATE FUNCTION public.rh_auditar_proyecto() RETURNS trigger
LANGUAGE plpgsql SET search_path = pg_catalog, public AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO public.bitacora_proyecto(proyecto_id, operacion, estado_anterior,
            estado_nuevo, usuario_aplicacion_id, rol_bd_sesion, motivo)
        VALUES (NEW.id, 'INSERT', NULL, NEW.estado,
            current_setting('app.actor_user_id')::bigint, session_user, NEW.motivo_decision);
    ELSIF NEW.estado IS DISTINCT FROM OLD.estado THEN
        INSERT INTO public.bitacora_proyecto(proyecto_id, operacion, estado_anterior,
            estado_nuevo, usuario_aplicacion_id, rol_bd_sesion, motivo)
        VALUES (NEW.id, 'CAMBIO_ESTADO', OLD.estado, NEW.estado,
            current_setting('app.actor_user_id')::bigint, session_user, NEW.motivo_decision);
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER rh_proyecto_auditoria AFTER INSERT OR UPDATE ON public.proyecto
FOR EACH ROW EXECUTE FUNCTION public.rh_auditar_proyecto();

-- Profundidad 2: trigger del proyecto -> trigger de inserción de bitácora.
-- Rechaza INSERT ordinario; no pretende defender frente al propietario que altera DDL.
CREATE FUNCTION public.rh_bitacora_origen() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF pg_trigger_depth() <> 2 THEN
        RAISE EXCEPTION 'Bitacora solo desde trigger de proyecto' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER rh_bitacora_origen BEFORE INSERT ON public.bitacora_proyecto
FOR EACH ROW EXECUTE FUNCTION public.rh_bitacora_origen();

CREATE FUNCTION public.rh_bitacora_inmutable() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'La bitacora es de solo insercion' USING ERRCODE = '23514';
END;
$$;
CREATE TRIGGER rh_bitacora_inmutable BEFORE UPDATE OR DELETE ON public.bitacora_proyecto
FOR EACH ROW EXECUTE FUNCTION public.rh_bitacora_inmutable();
CREATE TRIGGER rh_bitacora_no_truncate BEFORE TRUNCATE ON public.bitacora_proyecto
FOR EACH STATEMENT EXECUTE FUNCTION public.rh_bitacora_inmutable();
CREATE TRIGGER rh_proyecto_no_truncate BEFORE TRUNCATE ON public.proyecto
FOR EACH STATEMENT EXECUTE FUNCTION public.rh_bitacora_inmutable();

CREATE FUNCTION public.rh_validar_autor() RETURNS trigger
LANGUAGE plpgsql SET search_path = pg_catalog, public AS $$
DECLARE ficticio boolean;
BEGIN
    SELECT es_ficticio INTO ficticio FROM public.proyecto WHERE id = NEW.proyecto_id;
    IF NEW.medio_autorizacion = 'DATO_FICTICIO' AND ficticio IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'Autor ficticio requiere proyecto ficticio' USING ERRCODE = '23514';
    END IF;
    IF NEW.medio_autorizacion <> 'DATO_FICTICIO'
        AND nullif(btrim(NEW.version_autorizacion), '') IS NULL THEN
        RAISE EXCEPTION 'Version de autorizacion requerida' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER rh_autor_integridad BEFORE INSERT OR UPDATE ON public.proyecto_autor
FOR EACH ROW EXECUTE FUNCTION public.rh_validar_autor();

CREATE FUNCTION public.rh_actualizar_usuario() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.actualizado_en := clock_timestamp();
    RETURN NEW;
END;
$$;
CREATE TRIGGER rh_usuario_actualizado BEFORE UPDATE ON public.usuario
FOR EACH ROW EXECUTE FUNCTION public.rh_actualizar_usuario();

COMMENT ON FUNCTION public.rh_auditar_proyecto() IS
'Audita altas y transiciones desde V7; no reconstruye hechos previos. Actor local a la transaccion establecido por el servidor.';
