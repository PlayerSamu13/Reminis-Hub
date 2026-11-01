-- =====================================================================
-- ReminisHub - estructura base revisada para una BASE DE DATOS VACIA
-- Destino previsto: reminishub_test
--
-- IMPORTANTE:
-- 1) Este archivo crea tablas, restricciones e indices (equivalente al
--    esquema base V2). NO crea las funciones ni triggers de V3.
-- 2) Ejecutarlo solamente en una base de datos de pruebas vacia.
-- 3) Si Flyway ya aplico una V2 en otra base, NO reescribir esa migracion.
--    En ese caso, aplicar los cambios mediante una nueva migracion V3/V4.
-- 4) Los triggers pendientes deben implementar: auditoria, validacion de
--    transiciones, actualizado_en e inmutabilidad de es_ficticio.
-- =====================================================================

-- Limpieza de la tabla temporal creada por V1.
DROP TABLE IF EXISTS public.smoke;

-- ---------------------------------------------------------------------
-- usuario: cuenta de la aplicacion; el proveedor externo va separado.
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
                         id              bigint GENERATED ALWAYS AS IDENTITY,
                         email           varchar(254) NOT NULL,
                         nombre          varchar(120) NOT NULL,
                         password_hash   varchar(255),
                         rol             varchar(12) NOT NULL DEFAULT 'COLABORADOR',
                         activo          boolean NOT NULL DEFAULT true,
                         creado_en       timestamptz NOT NULL DEFAULT now(),
                         actualizado_en  timestamptz NOT NULL DEFAULT now(),
                         CONSTRAINT pk_usuario PRIMARY KEY (id),
                         CONSTRAINT uq_usuario_email UNIQUE (email),
                         CONSTRAINT ck_usuario_email CHECK (
                             email = lower(btrim(email))
                                 AND email ~ '^[^@[:space:]]+@[^@[:space:]]+$'
                             ),
                         CONSTRAINT ck_usuario_nombre CHECK (
                             nombre = btrim(nombre)
                                 AND char_length(nombre) BETWEEN 2 AND 120
                             ),
                         CONSTRAINT ck_usuario_rol CHECK (rol IN ('ADMIN', 'COLABORADOR')),
                         CONSTRAINT ck_usuario_password CHECK (
                             password_hash IS NULL
                                 OR char_length(password_hash) >= 50
                             )
);

COMMENT ON TABLE usuario IS
    'Cuenta de ReminisHub. Las cuentas nuevas deben recibir COLABORADOR; ADMIN se aprovisiona por un procedimiento controlado.';
COMMENT ON COLUMN usuario.password_hash IS
    'Hash generado por PasswordEncoder. NULL si la cuenta usa exclusivamente autenticacion externa.';
COMMENT ON COLUMN usuario.actualizado_en IS
    'Se inicializa aqui; V3 debe actualizarlo mediante trigger BEFORE UPDATE.';

-- ---------------------------------------------------------------------
-- identidad_externa: identidad estable del proveedor OAuth2/OIDC.
-- En v1 la aplicacion implementa Google; se puede ampliar en otra version.
-- ---------------------------------------------------------------------
CREATE TABLE identidad_externa (
                                   id               bigint GENERATED ALWAYS AS IDENTITY,
                                   usuario_id       bigint NOT NULL,
                                   proveedor        varchar(16) NOT NULL,
                                   subject          varchar(255) NOT NULL,
                                   correo_proveedor varchar(254),
                                   email_verificado boolean,
                                   vinculado_en     timestamptz NOT NULL DEFAULT now(),
                                   CONSTRAINT pk_identidad_externa PRIMARY KEY (id),
                                   CONSTRAINT fk_identidad_usuario FOREIGN KEY (usuario_id)
                                       REFERENCES usuario (id) ON DELETE CASCADE,
                                   CONSTRAINT ck_identidad_proveedor CHECK (proveedor IN ('GOOGLE')),
                                   CONSTRAINT ck_identidad_subject CHECK (
                                       subject = btrim(subject) AND char_length(subject) > 0
                                       ),
                                   CONSTRAINT ck_identidad_correo CHECK (
                                       correo_proveedor IS NULL
                                           OR (correo_proveedor = lower(btrim(correo_proveedor))
                                           AND correo_proveedor ~ '^[^@[:space:]]+@[^@[:space:]]+$')
                                       ),
                                   CONSTRAINT uq_identidad_proveedor_subject UNIQUE (proveedor, subject),
                                   CONSTRAINT uq_identidad_usuario_proveedor UNIQUE (usuario_id, proveedor)
);

COMMENT ON COLUMN identidad_externa.subject IS
    'Identificador estable del proveedor. Para Google OIDC corresponde al claim sub; nunca se usa el correo como identificador.';

-- ---------------------------------------------------------------------
-- categoria: codigo estable; las etiquetas se traducen en el front-end.
-- ---------------------------------------------------------------------
CREATE TABLE categoria (
                           id      bigint GENERATED ALWAYS AS IDENTITY,
                           codigo  varchar(30) NOT NULL,
                           activa  boolean NOT NULL DEFAULT true,
                           orden   smallint NOT NULL DEFAULT 0,
                           CONSTRAINT pk_categoria PRIMARY KEY (id),
                           CONSTRAINT uq_categoria_codigo UNIQUE (codigo),
                           CONSTRAINT ck_categoria_codigo CHECK (codigo ~ '^[A-Z][A-Z0-9_]*$'),
                           CONSTRAINT ck_categoria_orden CHECK (orden >= 0)
);

-- ---------------------------------------------------------------------
-- tecnologia: nombres propios; unicidad sin distinguir mayusculas.
-- ---------------------------------------------------------------------
CREATE TABLE tecnologia (
                            id      bigint GENERATED ALWAYS AS IDENTITY,
                            nombre  varchar(60) NOT NULL,
                            CONSTRAINT pk_tecnologia PRIMARY KEY (id),
                            CONSTRAINT ck_tecnologia_nombre CHECK (
                                nombre = btrim(nombre)
                                    AND char_length(nombre) BETWEEN 1 AND 60
                                )
);

CREATE UNIQUE INDEX uq_tecnologia_nombre_ci
    ON tecnologia (lower(nombre));

-- ---------------------------------------------------------------------
-- proyecto: estado actual y metadatos del proyecto academico.
-- ---------------------------------------------------------------------
CREATE TABLE proyecto (
                          id                    bigint GENERATED ALWAYS AS IDENTITY,
                          nombre                varchar(150) NOT NULL,
                          resumen               varchar(280) NOT NULL,
                          descripcion           text NOT NULL,
                          url_repositorio       varchar(300) NOT NULL,
                          url_demo              varchar(300),
                          anio                  smallint NOT NULL,
                          semestre              smallint NOT NULL,
                          asignatura            varchar(120),
                          categoria_id          bigint NOT NULL,
                          estado                varchar(20) NOT NULL DEFAULT 'PENDIENTE_REVISION',
                          motivo_decision       varchar(500),
                          cargado_por           bigint NOT NULL,
                          terminos_aceptados_en timestamptz NOT NULL,
                          terminos_version      varchar(30) NOT NULL,
                          es_ficticio           boolean NOT NULL DEFAULT false,
                          version               bigint NOT NULL DEFAULT 0,
                          creado_en             timestamptz NOT NULL DEFAULT now(),
                          actualizado_en        timestamptz NOT NULL DEFAULT now(),
                          CONSTRAINT pk_proyecto PRIMARY KEY (id),
                          CONSTRAINT fk_proyecto_categoria FOREIGN KEY (categoria_id)
                              REFERENCES categoria (id) ON DELETE RESTRICT,
                          CONSTRAINT fk_proyecto_cargado_por FOREIGN KEY (cargado_por)
                              REFERENCES usuario (id) ON DELETE RESTRICT,
                          CONSTRAINT ck_proyecto_nombre CHECK (
                              nombre = btrim(nombre)
                                  AND char_length(nombre) BETWEEN 3 AND 150
                              ),
                          CONSTRAINT ck_proyecto_resumen CHECK (
                              resumen = btrim(resumen)
                                  AND char_length(resumen) BETWEEN 30 AND 280
                              ),
                          CONSTRAINT ck_proyecto_descripcion CHECK (
                              char_length(btrim(descripcion)) BETWEEN 20 AND 4000
                              ),
                          CONSTRAINT ck_proyecto_url_repositorio CHECK (
                              url_repositorio = btrim(url_repositorio)
                                  AND url_repositorio ~ '^https://github\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$'
                                  AND url_repositorio !~* '\.git$'
                                  AND url_repositorio !~ '/\.{1,2}(/|$)'
                              ),
                          CONSTRAINT ck_proyecto_url_demo CHECK (
                              url_demo IS NULL
                                  OR (url_demo = btrim(url_demo)
                                  AND url_demo ~ '^https://[^[:space:]]+$')
                              ),
                          CONSTRAINT ck_proyecto_anio CHECK (anio BETWEEN 2000 AND 2100),
                          CONSTRAINT ck_proyecto_semestre CHECK (semestre IN (1, 2)),
                          CONSTRAINT ck_proyecto_asignatura CHECK (
                              asignatura IS NULL
                                  OR (asignatura = btrim(asignatura)
                                  AND char_length(asignatura) BETWEEN 2 AND 120)
                              ),
                          CONSTRAINT ck_proyecto_estado CHECK (
                              estado IN ('PENDIENTE_REVISION', 'PUBLICADO', 'RECHAZADO', 'RETIRADO')
                              ),
                          CONSTRAINT ck_proyecto_motivo CHECK (
                              (estado = 'RECHAZADO'
                                  AND motivo_decision IS NOT NULL
                                  AND char_length(btrim(motivo_decision)) > 0)
                                  OR (estado IN ('PENDIENTE_REVISION', 'PUBLICADO')
                                  AND motivo_decision IS NULL)
                                  OR estado = 'RETIRADO'
                              ),
                          CONSTRAINT ck_proyecto_terminos_version CHECK (
                              terminos_version = btrim(terminos_version)
                                  AND char_length(terminos_version) BETWEEN 1 AND 30
                              ),
                          CONSTRAINT ck_proyecto_version CHECK (version >= 0)
);

COMMENT ON COLUMN proyecto.terminos_aceptados_en IS
    'Fecha en la que el usuario cargador acepto los terminos aplicables al envio; no sustituye autorizaciones individuales de autores.';
COMMENT ON COLUMN proyecto.terminos_version IS
    'Identificador de la version exacta de los terminos aceptados; la version documental debe conservarse.';
COMMENT ON COLUMN proyecto.es_ficticio IS
    'Identifica datos de demostracion. V3 debe impedir su cambio por el rol ordinario de aplicacion.';
COMMENT ON COLUMN proyecto.version IS
    'Bloqueo optimista JPA (@Version). No debe incrementarse desde un trigger.';
COMMENT ON COLUMN proyecto.actualizado_en IS
    'V3 debe actualizarlo mediante trigger BEFORE UPDATE; Hibernate debe leer el valor generado.';

CREATE UNIQUE INDEX uq_proyecto_repositorio_activo
    ON proyecto (lower(url_repositorio))
    WHERE estado IN ('PENDIENTE_REVISION', 'PUBLICADO');

CREATE INDEX ix_proyecto_categoria
    ON proyecto (categoria_id);
CREATE INDEX ix_proyecto_cargado_por
    ON proyecto (cargado_por);
CREATE INDEX ix_proyecto_periodo
    ON proyecto (anio, semestre);
CREATE INDEX ix_proyecto_catalogo
    ON proyecto (anio DESC, semestre DESC, id DESC)
    WHERE estado = 'PUBLICADO';

-- ---------------------------------------------------------------------
-- proyecto_tecnologia: relacion muchos-a-muchos.
-- ---------------------------------------------------------------------
CREATE TABLE proyecto_tecnologia (
                                     proyecto_id   bigint NOT NULL,
                                     tecnologia_id bigint NOT NULL,
                                     CONSTRAINT pk_proyecto_tecnologia PRIMARY KEY (proyecto_id, tecnologia_id),
                                     CONSTRAINT fk_proyecto_tecnologia_proyecto FOREIGN KEY (proyecto_id)
                                         REFERENCES proyecto (id) ON DELETE CASCADE,
                                     CONSTRAINT fk_proyecto_tecnologia_tecnologia FOREIGN KEY (tecnologia_id)
                                         REFERENCES tecnologia (id) ON DELETE RESTRICT
);

CREATE INDEX ix_proyecto_tecnologia_inverso
    ON proyecto_tecnologia (tecnologia_id, proyecto_id);

-- ---------------------------------------------------------------------
-- proyecto_autor: solo se almacenan nombres que pueden mostrarse de acuerdo
-- con la autorizacion aplicable o datos sinteticos de demostracion.
-- Un autor real sin autorizacion no debe insertarse con su nombre.
-- ---------------------------------------------------------------------
CREATE TABLE proyecto_autor (
                                id                   bigint GENERATED ALWAYS AS IDENTITY,
                                proyecto_id          bigint NOT NULL,
                                usuario_id           bigint,
                                nombre_publico       varchar(120) NOT NULL,
                                orden                smallint NOT NULL,
                                autorizado_en        timestamptz,
                                version_autorizacion varchar(30),
                                medio_autorizacion   varchar(30) NOT NULL,
                                referencia_evidencia varchar(255),
                                CONSTRAINT pk_proyecto_autor PRIMARY KEY (id),
                                CONSTRAINT fk_proyecto_autor_proyecto FOREIGN KEY (proyecto_id)
                                    REFERENCES proyecto (id) ON DELETE CASCADE,
                                CONSTRAINT fk_proyecto_autor_usuario FOREIGN KEY (usuario_id)
                                    REFERENCES usuario (id) ON DELETE SET NULL,
                                CONSTRAINT uq_proyecto_autor_orden UNIQUE (proyecto_id, orden),
                                CONSTRAINT ck_proyecto_autor_nombre CHECK (
                                    nombre_publico = btrim(nombre_publico)
                                        AND char_length(nombre_publico) BETWEEN 2 AND 120
                                    ),
                                CONSTRAINT ck_proyecto_autor_orden CHECK (orden > 0),
                                CONSTRAINT ck_proyecto_autor_medio CHECK (
                                    medio_autorizacion IN (
                                                           'CUENTA_PROPIA',
                                                           'AUTORIZACION_DOCUMENTADA',
                                                           'DATO_FICTICIO'
                                        )
                                    ),
                                CONSTRAINT ck_proyecto_autor_evidencia CHECK (
                                    (medio_autorizacion = 'CUENTA_PROPIA'
                                        AND autorizado_en IS NOT NULL
                                        AND version_autorizacion IS NOT NULL
                                        AND referencia_evidencia IS NULL)
                                        OR
                                    (medio_autorizacion = 'AUTORIZACION_DOCUMENTADA'
                                        AND autorizado_en IS NOT NULL
                                        AND version_autorizacion IS NOT NULL
                                        AND referencia_evidencia IS NOT NULL
                                        AND char_length(btrim(referencia_evidencia)) > 0)
                                        OR
                                    (medio_autorizacion = 'DATO_FICTICIO'
                                        AND autorizado_en IS NULL
                                        AND version_autorizacion IS NULL
                                        AND referencia_evidencia IS NULL
                                        AND usuario_id IS NULL)
                                    )
);

CREATE UNIQUE INDEX uq_proyecto_autor_usuario
    ON proyecto_autor (proyecto_id, usuario_id)
    WHERE usuario_id IS NOT NULL;
COMMENT ON TABLE proyecto_autor IS
    'Autores visibles del proyecto. Para proyectos reales, cada nombre debe contar con autorizacion verificable antes de publicar. DATO_FICTICIO solo se permite en proyectos ficticios; V3 debe validar esta regla entre tablas.';
COMMENT ON COLUMN proyecto_autor.referencia_evidencia IS
    'Referencia interna protegida a la evidencia de autorizacion; nunca debe exponerse en la API publica.';

-- ---------------------------------------------------------------------
-- bitacora_proyecto: historial de altas y cambios de estado.
-- Contiene una FK estricta (RESTRICT) hacia el proyecto para forzar la
-- anonimizacion (Soft Delete) en casos de supresion legal de datos,
-- preservando asi la integridad referencial para Hibernate.
-- ---------------------------------------------------------------------
CREATE TABLE bitacora_proyecto (
                                   id                    bigint GENERATED ALWAYS AS IDENTITY,
                                   proyecto_id           bigint NOT NULL,
                                   operacion             varchar(20) NOT NULL,
                                   estado_anterior       varchar(20),
                                   estado_nuevo          varchar(20) NOT NULL,
                                   usuario_aplicacion_id bigint,
                                   rol_bd_sesion         varchar(63) NOT NULL,
                                   motivo                varchar(500),
                                   registrado_en         timestamptz NOT NULL DEFAULT clock_timestamp(),
                                   CONSTRAINT pk_bitacora_proyecto PRIMARY KEY (id),
                                   CONSTRAINT fk_bitacora_proyecto FOREIGN KEY (proyecto_id)
                                       REFERENCES proyecto (id) ON DELETE RESTRICT,
                                   CONSTRAINT fk_bitacora_usuario FOREIGN KEY (usuario_aplicacion_id)
                                       REFERENCES usuario (id) ON DELETE SET NULL,
                                   CONSTRAINT ck_bitacora_operacion CHECK (
                                       operacion IN ('INSERT', 'CAMBIO_ESTADO')
                                       ),
                                   CONSTRAINT ck_bitacora_estados CHECK (
                                       estado_nuevo IN ('PENDIENTE_REVISION', 'PUBLICADO', 'RECHAZADO', 'RETIRADO')
                                           AND (estado_anterior IS NULL OR estado_anterior IN (
                                                                                               'PENDIENTE_REVISION', 'PUBLICADO', 'RECHAZADO', 'RETIRADO'
                                           ))
                                       ),
                                   CONSTRAINT ck_bitacora_coherencia CHECK (
                                       (operacion = 'INSERT' AND estado_anterior IS NULL)
                                           OR
                                       (operacion = 'CAMBIO_ESTADO'
                                           AND estado_anterior IS NOT NULL
                                           AND estado_anterior <> estado_nuevo)
                                       ),
                                   CONSTRAINT ck_bitacora_motivo_rechazo CHECK (
                                       estado_nuevo <> 'RECHAZADO'
                                           OR (motivo IS NOT NULL AND char_length(btrim(motivo)) > 0)
                                       )
);

CREATE INDEX ix_bitacora_proyecto
    ON bitacora_proyecto (proyecto_id, registrado_en);
CREATE INDEX ix_bitacora_usuario
    ON bitacora_proyecto (usuario_aplicacion_id)
    WHERE usuario_aplicacion_id IS NOT NULL;

COMMENT ON COLUMN bitacora_proyecto.proyecto_id IS
    'Clave foranea estricta hacia proyecto. Protegida con RESTRICT para forzar soft-delete/anonimizacion y evitar EntityNotFoundException en el ORM.';
COMMENT ON COLUMN bitacora_proyecto.usuario_aplicacion_id IS
    'Usuario autenticado transmitido por contexto transaccional. NULL cuando no hay actor de aplicacion.';
COMMENT ON COLUMN bitacora_proyecto.rol_bd_sesion IS
    'Identidad tecnica de la conexion PostgreSQL (session_user), no la persona autenticada.';

-- NOTA: esta migracion no concede privilegios ni crea triggers.
-- Hacerlo en una migracion posterior (V3), con roles de BD separados.