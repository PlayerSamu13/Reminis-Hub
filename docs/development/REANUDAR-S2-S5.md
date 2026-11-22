# ReminisHub — instrucción de reanudación S2–S5

Continúa en `C:\Users\samuq\Proyecto PW`. Lee `AGENTS.md`, `C:\Users\samuq\Downloads\PROMPT_INICIO.md` y el inicio de `docs/development/trazabilidad.md`. Inspecciona el árbol de trabajo, no solo HEAD. Este documento conserva contexto; verifica cualquier estado que pueda haber cambiado.

## Autorización y preferencias vigentes

- El estudiante aprobó explícitamente S2–S5: integridad/auditoría, presentación, CRUD/moderación y reporte. No repidas la petición de aprobación de ese alcance. Una aprobación del plan no certifica que una etapa funcione.
- Se mantiene Thymeleaf, PostgreSQL nativo, Java 25/Boot 4.1.2 y dependencias existentes. Verifica pom y documentación oficial antes de APIs nuevas.
- Código simple y defendible; estilo minimalista inspirado en GitHub. Documentación JavaDoc y Swagger sobre comportamiento real.
- Git: commits locales de seguimiento autorizados históricamente, con mensajes breves como «Creación de...» o «Corrección de...». No push. Respeta los hitos de cierre de AGENTS y no confirmes trabajo ajeno incidentalmente.
- El estudiante prefirió estudiar mediante `docs/defensa/guia-sustentacion.md`; conserva explicaciones, evidencias y ejercicios reservados. No hagas en su lugar el ejercicio pendiente de GET /login con nuevo CSRF después de logout salvo que lo pida.
- Nunca leer/imprimir .env, secretos, cookies o JWT. No modificar V1–V6 aplicadas; no clean/repair/reset/rebase destructivo.
- Única base autorizada para pruebas: `jdbc:postgresql://localhost:5432/reminishub_test`. `reminishub_dev` es producción según el estudiante. No probar allí.

## Resultado vigente del 21/11/2026

La ejecución d4f2629178474a8e82490ade9865b2a1 confirmó las 116 pruebas con PostgreSQL. Los cambios posteriores de interfaz y CSV pasaron sus pruebas específicas. El estado de entrega actualizado se encuentra al inicio de trazabilidad.md. Las siguientes entradas conservan el historial.

## Historial de la corrección de auditoría

La suite PostgreSQL de 116 pruebas dio 1 fallo y 2 errores (sufijo 3f1a9caf80434806b097a5412bebeffe). Se corrigieron los fixtures/configuración: Flyway con datasource propio en AuditoriaIntegrationTests para no agotar el pool JPA de tamaño uno; IP simulada distinta por caso de SesionE2ETests para evitar 429 por contaminación entre pruebas. Segunda ejecución a8a943012a7a45c187b035add1cf9e27: sesión PASS; auditoría falla por credenciales ausentes en su datasource separado. Boot 4.1.2 no las hereda en este flujo (fuente local inspeccionado). Se añadieron suppliers de DB_USER y DB_PASSWORD a spring.flyway.user/password; pendiente nueva ejecución. Repetir verificar.ps1 y leer el resultado antes de declarar cierre. Ver trazabilidad para causas y evidencia.

## Historial de implementación (20/11/2026)

S2–S5 ya tienen implementación. No repetir su creación: leer el Estado actual de trazabilidad.md y operacion-propuestas.md. V7 fue trasladada a recursos Flyway; las referencias siguientes a candidata externa son historia previa. La regresión local s2-s5-regresion.log registra 86 PASS, sin pruebas PostgreSQL. Primer pendiente: ejecutar scripts/verificar.ps1 en reminishub_test y revisar errores reales; después completar los recorridos de escritura; el recorrido HTTPS de lectura ya pasó con ADMIN (CSS, propuestas, reporte, bitácora y moderación ES/EN). El estudiante autorizó cierre solo si se verifica completamente. Mantener cambios ajenos y documentar resultados reales.

## Estado histórico al preparar esta instrucción (20/11/2026)

- Rama `codex/google-oidc`, último commit `a6ca760`. Árbol sucio con cambios previos de autenticación, registro, documentación y scripts; preservar `.env.example` ajeno. No se crearon commits en estas últimas revisiones.
- Catálogo público, registro, autenticación local/Google y logout existen; hay correcciones pendientes de cierre. Última regresión S1b: 55 pruebas PASS; después TokenServiceTests 9/9 en otra ejecución. No sumarlas como una suite única.
- V1–V6 están en recursos Flyway. **V7 todavía NO está en recursos ni aplicada permanentemente.**
- Candidata: `docs/development/sql/V7__integridad_y_auditoria_proyectos.sql`. Verificador: `docs/development/sql/verificar_s2.sql`.
- El estudiante ejecutó el verificador en PostgreSQL real: PASS de alta, actor, transiciones, rechazo de actor ausente, inmutabilidad y retiro final; luego ROLLBACK y PASS del contexto local. Evidencia aportada por el estudiante, no ejecución propia del agente. No acredita pool JPA ni privilegios completos.
- Un primer intento falló por un espacio final del resumen del fixture. Se corrigió con btrim; NO relajar el constraint de V2. El segundo intento pasó.
- Nuevos archivos Java: `service/ActorProyecto.java` y `AuditoriaIntegrationTests.java`. Actor usa JWT del SecurityContext, cuenta activa/rol vigente y EntityManager para set_config local en transacción MANDATORY. La prueba usa pool de una conexión y rollback, aún sin ejecución DB.
- La prueba JPA requiere V7 en el classpath/aplicada: **no ejecutarla todavía suponiendo que el PASS del script dejó V7 instalada**.
- `web_user` no es superusuario ni puede crear roles/bases, según salida aportada. Propiedad de tablas y privilegios pendientes.
- El estudiante confirmó app detenida para probar S2. Reconfirmar estado actual antes de compilar/promover V7 si pasó tiempo: DevTools podría migrar al reiniciar.
- DB_* no estaban disponibles al agente. psql llegó a PostgreSQL fuera del sandbox y falló por contraseña ausente. No pedir contraseña en chat.

## Continuación inmediata de S2

1. Revisar candidata y prueba Java. Resolver permisos de bitácora: UPDATE/DELETE/TRUNCATE están bloqueados por triggers, pero INSERT directo y capacidad del propietario para desactivar triggers no quedan neutralizados. No prometer inviolabilidad. No crear roles ni SECURITY DEFINER con permisos amplios sin diseño y revisión.
2. Añadir pruebas faltantes: autores ficticios en proyecto real, autorización vacía, actor ajeno/inactivo, timestamps, fallo con rollback completo. Verificar que un rechazo procede de la regla prevista: el helper actual acepta cualquier SQLSTATE 23514 y podría dar un falso positivo por otro CHECK.
3. No alterar retroactivamente la historia de los ocho ejemplos V5. No inventar consentimientos. Validar datos existentes antes de migrar.
4. Tras revisar V7, promover UNA fuente a `src/main/resources/db/migration`, actualizar la referencia del script y documentar cómo ejecutarlo cuando V7 ya exista. No dejar copias divergentes. Aplicar únicamente mediante Flyway en test; nunca psql suelto como instalación permanente.
5. Ejecutar `AuditoriaIntegrationTests` y regresión PostgreSQL. La prueba exige transacción existente, inserta mediante conexión JPA, comprueba actor en bitácora, revierte y verifica misma conexión física sin actor ni filas persistidas. Revisar que fixtures antiguos no fallen por nuevos triggers; no debilitar reglas para acomodarlos.
6. Actualizar `scripts/verificar.ps1` para incluir la nueva suite sin aceptar XML obsoletos. Leer resultados Surefire de la ejecución actual. Mantener PASS/FAIL/NOT RUN/BLOCKED separados.
7. Revisión final, documentación y commit exclusivo del incremento aceptado; no incluir todos los cambios con git add . .

## Incrementos siguientes ya autorizados

**S3 — presentación:** DTO/formulario validado, servicio transaccional, API y Thymeleaf ES/EN. Crear PENDIENTE_REVISION; propietario/actor/rol y versión de términos fijados en servidor. Autores internos o externos con autorización documentada sin subir archivos; tecnologías relacionales y categoría activa. No aceptar DATO_FICTICIO desde formulario público. Validar URL GitHub según contrato SQL. Probar 401/403, CSRF, campos manipulados y rollback de relaciones/bitácora. Swagger documenta entradas/respuestas reales.

**S4 — CRUD y moderación:** Mis propuestas y lectura privada; edición del propietario solo pendiente/rechazado, baja lógica según D7, @Version con conflicto 409. ADMIN lista pendientes, aprueba/rechaza con motivo y revisa consentimientos. Bitácora administrativa paginada y DTO que no exponga evidencia privada al público. RF-12 reenvío es opcional: que el trigger permita la transición no autoriza agregar la función a la interfaz. Probar otro propietario, rol insuficiente, concurrencia y estados terminales. GET nunca escribe.

**S5 — reporte:** consultas JdbcTemplate parametrizadas y agregaciones contrastadas con SQL directo; filtros acotados, vista ES/EN y exportación CSV ADMIN. Escapar comillas/separadores/saltos y mitigar fórmulas según consumidor, con pruebas. Ceros/estado vacío accesibles. Nada de cifras fijas, PDF ni dependencias nuevas por rutina.

En cada incremento: controladores delgados, servicio transaccional, DTO externo (record si conviene; formulario mutable permitido), constructor injection, errores sin trazas, permisos en servidor, CSRF activo, JavaDoc útil, OpenAPI y pruebas reales. No afirmar conformidad WCAG solo por puntaje. Actualizar guía de defensa con decisiones y recorrido de petición.

## Comandos y límites operativos

Desde el módulo, JDK válido en esta máquina:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.5'
.\mvnw.cmd -o -DskipTests test-compile
```

PASS propio al preparar la instrucción: comando anterior, salida 0. Compilar NO verifica PostgreSQL. Log: `target/reanudacion-s2-compilacion.log`. Repetir solo si hay cambios que lo justifiquen. Maven necesita acceso al caché .m2 fuera del sandbox; solicitar escalación si se bloquea, no cambiar dependencias/repositorio por ese motivo.

Desde raíz, con variables privadas ya configuradas en la terminal del estudiante:

```powershell
.\scripts\verificar.ps1 -Offline
```

No publicar valores privados. Antes de ejecutar suite/arranque, mostrar URL test resuelta y contraseña oculta. No arrancar producción. Para S2 antes de promoción, el comando usado exitosamente fue:

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -X -W -h localhost -p 5432 -U web_user -d reminishub_test -f '.\docs\development\sql\verificar_s2.sql'
```

No repetirlo ciegamente una vez aplicada V7: CREATE FUNCTION/CREATE TRIGGER encontrarán objetos existentes. El rollback puede dejar huecos en secuencias; eso no implica pérdida de datos.

## Plazo y honestidad

Fecha de preparación 20/11, congelamiento interno hoy y entrega declarada 23/11. S3–S5 no existen todavía; no prometer que el alcance completo ya está listo. Quedan además validación UI, documentación y estudio real con cinco participantes. La IA no sustituye participantes ni evidencia. Priorizar recorrido mínimo completo y elevar el riesgo de plazo; no recortar Must silenciosamente.

Primera respuesta al reanudar: informar el estado comprobado brevemente, continuar S2 desde el primer pendiente real y trabajar dentro de la autorización S2–S5 sin otra planificación extensa. Si falta acceso DB, avanzar revisión/código/pruebas independientes y entregar el comando concreto que desbloquea la verificación; no inventar PASS ni cerrar etapas incompletas.
