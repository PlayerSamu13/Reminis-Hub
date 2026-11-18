# ReminisHub — trazabilidad

## Estado actual — 18/11/2026

- HTTPS cerrado tras defensa corregida; registro local aprobado y cerrado (ficha etapa-02b); autenticación local E3A APROBADA por delegación del estudiante.
- Rama: codex/registro-cierre (registro y autenticacion aun sin commit).
- Stack confirmado nuevamente: Thymeleaf y PostgreSQL.
- Base autorizada de pruebas: jdbc:postgresql://localhost:5432/reminishub_test.
- reminishub_dev es produccion segun el estudiante; excluirla de pruebas.
- Arranque: Maven Wrapper spring-boot:run con variables locales configuradas.
- Pruebas: scripts/verificar.ps1 -Offline desde la raiz; ahora exige RH_JWT_SECRET (>=32 bytes) en lugar de DEV_USER_PASSWORD.
- D1–D7 resueltas conforme a la seccion siguiente.
- HTTPS: verificacion automatica PASS; navegador confirmado por el estudiante.
- Ejercicio de estado vacio EN revisado; regresion PostgreSQL PASS, 14 pruebas.
- Estabilizacion: defensa revisada; teclado/Swagger confirmados por el estudiante.
- Registro: suite completa 12 web + 5 servicio PASS; revision visual y ficha cerradas.
- Login/JWT: Suite previa de 60 PASS (644eaccd25f3465483187723480a57bb). Nueva revisión de vigencia: 40 pruebas sin DB PASS. Revisión posterior: 42 pruebas sin DB PASS, 64/64 con PostgreSQL y V6 PASS. Sufijo f2096d038ae849b59b982f51e8175c17, once XML y 64 casos individuales verificados.
- Preparacion de defensa escrita por decision del estudiante: docs/defensa/guia-sustentacion.md; sin exigir respuestas por etapa.
- Navegador: 16 combinaciones de vistas/idioma/ancho revisadas; accesibilidad del formulario limpio corregida.

### Revocación y límite de intentos — 18/11/2026

Implementados jti por sesión, tabla V6 de revocaciones, rechazo de cookies anteriores sin jti, respuesta 429 traducida y ventana por correo de una instancia. PASS de Maven en cinco suites sin PostgreSQL (42 casos) y ocho regresiones simuladas del verificador. E2E actualizado para dos sesiones, con rechazo de la cerrada y continuidad de la otra. BLOCKED la ejecución completa actual de 64 pruebas. PostgreSQL requiere contraseña y este proceso carece de las variables de conexión. Los informes sintéticos del verificador no acreditan la integración. Pendientes validación de V6 y aprobación antes de Google.

### Vigencia de cuentas — 18/11/2026

La siguiente petición con JWT válido consulta el usuario por correo y exige cuenta activa y rol coincidente. Una cuenta ausente tampoco obtiene identidad. Si se restaura el estado original, una copia aún vigente puede volver a funcionar. La duración admitida es de 1 a 60 minutos. La consulta adicional tiene un coste por petición. Pendientes la revocación al salir y los límites de intentos. Maven ejecutó 40 pruebas sin PostgreSQL con cero fallos, errores u omisiones. La ejecución PostgreSQL de 60 corresponde a la versión anterior.

### Revision y refinamiento — 18/11/2026

- Autorizacion actual: revisar la etapa creada ayer, refinar interfaz/legibilidad y elaborar una guia para estudiar al finalizar. No se exige contestar una defensa por etapa; no implica aprobacion automatica de funcionalidades pendientes.
- [Verificado: Surefire] sufijo `effa2ed51b7b40d8b1f4c36b5dc2a1ee`: diez suites, 58 pruebas, cero fallos, errores u omisiones; incluye RegistroE2ETests (1) y SesionE2ETests (3). Corrige los NOT RUN historicos mas abajo para esa version, no para cambios posteriores.
- Hallazgo MEDIO corregido: GET /api/v1/sesion con Accept text/html devolvia 302. Prueba nueva reproducida FAIL (esperado 401, obtenido 302); se prioriza el matcher /api/** y ahora pasa. No es una escalada de privilegios, sino un contrato HTTP incorrecto.
- Login: error asociado a ambos campos, estilo invalid y foco unico en correo; prueba nueva verifica descripcion/foco y ausencia de la clave. Formularios: campos 44 px, texto 16 px y etiquetas 14 px; cabecera adaptable y nombres largos acotados. Sin dependencia ni capa nueva.
- Pruebas E2E: limpieza antes por prefijo (podia afectar otra ejecucion); ahora borra solo el correo UUID de la prueba actual. Compilacion PASS; ejecucion PostgreSQL de esta correccion NOT RUN porque DB_* no estan configuradas en esta terminal. No se borraron cuentas durante la revision.
- Verificador: ahora exige RegistroE2ETests, antes omitida de los minimos, y 15 pruebas de autenticacion web. Minimo total: 60. Ocho regresiones simuladas PASS.
- PASS actual: `mvnw.cmd -o -Dtest=TokenServiceTests,AutenticacionWebTests,RegistroWebTests,RegistroServiceTests test`: 38 pruebas, cero fallos/errores/omisiones; Surefire confirmado. `mvnw.cmd -o -DskipTests test-compile`: PASS tras modificar la limpieza E2E.
- PASS navegador: catalogo, detalle /proyectos/8, login y registro; ES/EN a 1366/390 px (16 casos), sin desbordamiento ni referencias aria-describedby ausentes. Axe: cero infracciones detectadas y una comprobacion manual por caso. Capturas de login movil y registro escritorio inspeccionadas. Evidencia local: `reminis-hub/target/refinamiento/informe.json`. No demuestra todos los estados ni conformidad WCAG completa.
- Limites abiertos: token copiado valido tras logout; rol/activo del JWT no se reconsultan tras cambios en DB; sin limitacion de intentos; duracion JWT configurable sin validacion de rango. Resolver antes de operaciones administrativas o exposicion publica. No se introduce revocacion ni nuevas tablas sin definir ese contrato.
- Guia nueva: arquitectura, recorridos, decisiones, diferencias entre firma/hash/TLS, preguntas con respuestas y guion de demostracion. Corregidos JavaDoc que confundian emisor/audiencia y HttpOnly/Secure.
- Git: se conserva codex/registro-cierre y el trabajo previo; no se realiza commit de etapa no aceptada. El plazo sigue en riesgo: al 18/11 faltan Google, propuestas/moderacion, auditoria y reporte, ademas de usabilidad real; no se promete terminarlo todo antes del congelamiento del 20/11.

Defensa HTTPS completada: el estudiante distingue acceso JavaScript a cookie Secure sin HttpOnly y cifrado sin verificacion de identidad con curl -k. Se acepto continuar al registro. Ficha etapa-02a.md. Consulta publica sigue abierta; solo las acciones de colaboracion requieren cuenta. RF-05 incluye registro local y login, RF-06 Google, ambos obligatorios segun la matriz acordada.

Actualizacion HTTPS: [Declarado por el estudiante] confianza del navegador validada. [Verificado: keytool -printcert sobre localhost.crt] SAN localhost/127.0.0.1, RSA 3072, SHA256withRSA, uso serverAuth, CA=false; vigencia del 16/11/2026 14:59:51 al 16/12/2026 14:59:51 America/Bogota. La fecha de entrega 23/11 queda dentro de vigencia. Defensa completada y corregida, documentada en etapa-02a.md.

### Registro local: plan aprobado, implementacion en revision, 3–4 h del estudiante

Objetivo RF-05 parcial y criterios 1/2/4: GET/POST /registro, formulario ES/EN y persistencia de colaborador con BCrypt. La tabla usuario exige correo normalizado y unico, nombre valido, rol y activo. Aprovechar constraints existentes; sin migracion prevista. No incluir login/JWT en este hito.

Archivos previstos: UsuarioRepository, RegistroService, formulario de registro mutable para binding, controlador web, plantilla registro.html, bundles, SecurityConfig y pruebas unitarias/integracion. Error de correo duplicado comprensible, sin trazas; normalizar antes de guardar y tratar tambien la carrera de unicidad. Rol COLABORADOR y activo fijados por servidor; formulario limitado a nombre, correo y contrasena. No devolver ni repoblar contrasena/hash. CSRF activo y ejecucion del formulario sobre HTTPS.

Aceptacion: registro valido y hash comprobado mediante PasswordEncoder.matches; rechazo de campos invalidos, correo duplicado normalizado y manipulacion de rol; CSRF sin token 403; regresion de catalogo y errores ES/EN. PostgreSQL exclusivamente reminishub_test, fixtures reversibles. Tu turno reservado: prueba de rol ADMIN enviado por cliente; debe persistir COLABORADOR. Miniclase: binding, validacion, hash y transaccion. Defensa creciente: carrera de dos registros iguales, limite entre DTO y autorizacion, diferencia entre TLS y hash. Plan aprobado expresamente; etapa aun no cerrada.

#### Evidencia de registro — 17/11/2026

- [Verificado: codigo] GET/POST /registro, formulario mutable con campos permitidos, rol COLABORADOR/activo fijados en servicio, BCrypt coste 12, validacion y correo normalizado. Contrasena minimo 15 unidades UTF-16, maximo 72 bytes UTF-8; nunca se repuebla en HTML. Sin migraciones ni dependencias nuevas. Login/JWT/Google pendientes.
- PASS: `mvnw.cmd -o -Dtest=RegistroServiceTests,RegistroWebTests test`, codigo 0. Surefire: 5 servicio + 6 web; cero fallos, errores u omisiones. Finalizado 19:43:11 -05:00. Prueba HTTPS usa esquema https real en MockMvc; idioma del POST viene de cookie seleccionada mediante GET.
- PASS: `scripts/probar-verificador.ps1`, siete casos simulados, sin PostgreSQL. El verificador exige ahora seis suites, minimo 29 pruebas, y solo acepta la URL autorizada. Esto no demuestra funcionamiento de la DB.
- PASS: ejecucion del estudiante con `scripts/verificar.ps1 -Offline`; informes Surefire sufijo 4445fe5d1e95455da41c2c39f1d66bbe inspeccionados: 29 pruebas, cero fallos, errores u omisiones, seis suites. RegistroIntegrationTests aporta cuatro pruebas. La prueba validacionRolBaseDatos envia rol=ADMIN y comprueba COLABORADOR mediante SQL; ejercicio correcto. La prueba de duplicado simula consulta previa desactualizada con INSERT/constraint reales, no concurrencia entre hilos.
- BLOCKED: recorrido real y auditoria visual de registro; curl a localhost:8443 no encontro servidor. No se declara conformidad WCAG ni PASS de extremo a extremo.
- Pendientes: navegador ES/EN/errores/teclado, auditoria de accesibilidad, defensa y APROBADA. Cambios en codex/registro-local sin commit de etapa; conservar .env.example preexistente.

Actualizacion autorizada de botones (17/11): acceso Crear cuenta en catalogo con btn btn-primary; Explorar proyectos en formulario con btn btn-outline-secondary y, tras exito, btn btn-primary. Acciones agrupadas con flex-wrap/gap para adaptar la distribucion. Se conservan enlaces de navegacion, submit, traducciones y estilos compartidos; sin dependencias nuevas. Preferencia visual vigente: minimalismo moderno inspirado en GitHub. PASS: RegistroWebTests, seis pruebas sin fallos, errores u omisiones; Surefire y Maven codigo 0, 20:53:52 -05:00. PASS git diff --check. Revision visual real BLOCKED: localhost:8443 rechaza conexion. Esta ejecucion no repite las 29 pruebas PostgreSQL anteriores.

Revision de cierre solicitada (defensa aplazada por autorizacion expresa): nucleo funcional respaldado por las pruebas existentes; no declarar registro completamente terminado. Hallazgo BAJO verificado: registro.html referencia IDs de errores mediante aria-describedby, pero SpringErrorsTagProcessor de Thymeleaf 3.1.5.RELEASE elimina los elementos th:errors cuando no hay errores (fuente local del JAR inspeccionada). Propuesta: conservar contenedores de descripcion o emitir referencias condicionales. Cobertura pendiente: seguir POST exitoso hasta GET de confirmacion y recarga; las pruebas actuales solo verifican redireccion y flash. Limite transaccional: las pruebas PostgreSQL consultan dentro de transacciones de prueba revertidas, no verifican durabilidad mediante una nueva transaccion tras commit de la peticion. Recorrido navegador, cookies efectivas y auditoria visual/accesible BLOCKED por servidor 8443 inaccesible. Los resultados 29/29 y 6/6 se releyeron de Surefire; no son nuevas ejecuciones en esta revision. No se modifico codigo funcional ni se aprobo la etapa.

## Orden de entrega revisado — 16/11/2026

Actualizacion posterior de revision real: curl fuera del sandbox devuelve HTTP 200 en https://localhost:8443/registro con validacion TLS activa; dentro del sandbox rechaza conexion. Los rechazos previos no prueban que el servidor del estudiante estuviera detenido. Edge headless sin aceptar certificados invalidos: ES/EN a 1366 y 390 px emulados, sin desbordamiento horizontal; botones de 43 px, estilos principal/secundario aplicados. Tab alcanza campos y acciones con contorno visible de 3 px. Navegacion catalogo -> registro -> catalogo PASS. Axe (tags WCAG A/AA seleccionados): cero infracciones detectadas y dos revisiones manuales por vista; no equivale a conformidad completa. Inspeccion DOM confirma cuatro referencias aria-describedby sin destino en formulario limpio. Evidencia local ignorada por Git: reminis-hub/target/revision-registro/informe.json y capturas. No se enviaron formularios ni se crearon cuentas; confirmacion, cookies y persistencia tras commit siguen fuera de esta comprobacion. Defensa aplazada.

### Cierre de accesibilidad, pruebas y documentacion del registro — 17/11/2026

- [Verificado: codigo] `registro.html` emite `aria-describedby` solo cuando el mensaje existe y aplica `is-invalid`/`invalid-feedback` de Bootstrap; `layout.html` publica `meta description` localizada por vista; `README.md` documenta el arranque HTTPS y advierte que `spring-boot:run` sin perfil deja `/registro` inaccesible. Rama `codex/registro-cierre`.
- PASS: `RegistroWebTests` 8/8 y `RegistroServiceTests` 5/5 con `mvnw.cmd -o -Dtest=RegistroWebTests,RegistroServiceTests test` (Surefire 22:34, cero fallos, errores u omisiones). Las dos pruebas nuevas afirman que el formulario limpio no referencia errores inexistentes y que el invalido marca el campo y solo referencia mensajes presentes.
- PASS: comprobacion HTTP en `https://localhost:8443`; formulario limpio con `aria-describedby="clave-ayuda"` y sin referencias colgantes; tras 400 los tres campos quedan con `is-invalid`, borde `rgb(220,53,69)` y referencias existentes. Lighthouse: accesibilidad 1, buenas practicas 1 y SEO 1 en `/proyectos` y `/registro` (antes 0.75 por falta de `meta description`).
- PASS: paridad de bundles 87/87 y `git diff --check`.
- NOT RUN: `RegistroE2ETests`, que exige `CATALOGO_DB_TESTS=true` y PostgreSQL real. Limpia sus cuentas `e2e-registro-%@example.invalid` en `@AfterEach` y comprueba 302, flash de un solo uso y fila persistida tras el commit.
- Nota de entorno: este shell tenia `JAVA_HOME` apuntando a un JRE 17 y Maven fallaba con "No compiler is provided"; la ejecucion uso un JDK 25 solo para ese comando. Corregir `JAVA_HOME` de forma permanente evita repetir el fallo.
- Pendiente: `APROBADA`, ficha de defensa y commit. El registro sigue sin login/JWT.

#### Revision del ejercicio y aprobacion — 17/11/2026

- [Verificado: codigo] El estudiante implemento el foco al primer campo invalido con `th:autofocus` condicional y sin JavaScript. Prioridad correcta: nombre, luego correo, luego contrasena; nunca mas de un campo recibe el foco.
- Hallazgo corregido: `th:autofocus` de la contrasena solo miraba `hasErrors('contrasena')`, asi que un error exclusivo de bytes (mas de 72 bytes UTF-8) dejaba la pagina sin foco. Se agrego `hasErrors('contrasenaCompatible')` a la condicion.
- PASS: `RegistroWebTests` 12/12 y `RegistroServiceTests` 5/5; 17 pruebas, cero fallos, errores u omisiones (`mvnw.cmd -o -Dtest=RegistroWebTests,RegistroServiceTests test`, codigo 0). Cuatro pruebas nuevas fijan el comportamiento del foco: cero en formulario limpio, uno y en el campo correcto para nombre, correo, clave corta, solo bytes y los tres errores.
- PASS navegador real (Chromium, https://localhost:8443): con error de bytes el foco queda en `contrasena` y `aria-describedby` resuelve `clave-ayuda` y `clave-bytes-error`; con tres errores el foco queda en `nombre` y hay un solo `autofocus`; en formulario limpio no hay `autofocus` y el foco permanece en `BODY`. Lighthouse `/registro`: accesibilidad 1, buenas practicas 1, SEO 1, sin fallos.
- Nota metodologica: la primera comprobacion en vivo dio falso negativo porque `target/classes` conservaba la plantilla anterior; `spring-boot:run` sirve esa copia, no `src/main/resources`. Tras `process-resources` la comprobacion coincidio con las pruebas. Verificar siempre sobre la copia compilada.
- APROBADA por el estudiante con comprension aplazada. Ficha `docs/defensa/etapa-02b.md`. Sin commit: los cambios siguen compartidos entre ramas.

Stack confirmado: Thymeleaf y PostgreSQL. La estabilizacion quedo cerrada con 14 pruebas verificadas y recorrido de teclado/Swagger declarado por el estudiante. Commit 92f6524. El cambio previo de .env.example sigue local, fuera de ese commit.

### Autenticacion local E3A: login, JWT y logout - 17/11/2026

- [Verificado: codigo] `JwtConfig` firma HS256 con Nimbus (ya presente por el starter OAuth2: sin dependencia nueva) y valida firma, emisor `reminishub` y expiracion. El claim `rol` solo concede autoridad si coincide con `RolUsuario`; un rol inventado queda sin permisos. El secreto entra por `RH_JWT_SECRET` y el arranque falla si mide menos de 32 bytes.
- [Verificado: codigo] Cookie `RH_TOKEN`: `HttpOnly`, `Secure`, `SameSite=Lax`, `Path=/`, 60 min (`RH_JWT_MINUTOS`). No se guarda en `localStorage` ni se expone a JavaScript. `spring.security.user.*` eliminado: ya no existe usuario provisional en memoria.
- [Verificado: codigo] `POST /login` valida con `DaoAuthenticationProvider` + BCrypt y emite el JWT; un correo inexistente, una clave incorrecta y una cuenta inactiva devuelven el mismo 401 traducido, sin repoblar la contrasena. `POST /logout` con CSRF borra la cookie. `GET /api/v1/sesion` responde 401 sin token y los datos del token con el.
- Hallazgo grave corregido: `oauth2ResourceServer` exime de CSRF toda peticion con bearer token porque supone que viaja en cabecera. Con el token en cookie, cualquier POST autenticado quedaba sin proteccion. Evidencia en traza: `Did not protect against CSRF since request did not match ... Not [OAuth2ResourceServerConfigurer$BearerTokenRequestMatcher]`. Se sustituyo por un filtro propio colocado despues de `CsrfFilter`, de modo que el CSRF se valida antes de leer el JWT.
- Segundo hallazgo: el filtro estandar de logout interceptaba `POST /logout` y redirigia a `/login?logout`. Se desactivo y el cierre lo hace `LoginController`.
- PASS: `mvnw.cmd -o -Dtest=TokenServiceTests,AutenticacionWebTests,RegistroWebTests,RegistroServiceTests test`, 36 pruebas (6 token, 13 web de acceso, 12 registro web, 5 servicio), cero fallos, errores u omisiones, codigo 0. Incluye token con otra clave, token expirado, rol inventado, atributos de cookie, 401 de API, token alterado, logout sin CSRF (403 y la cookie sigue sirviendo), flujo real de CSRF con sesion tomando el valor impreso en el HTML, y cuenta inactiva.
- PASS: paridad de bundles 100/100 y `git diff --check`. PASS: `scripts/probar-verificador.ps1`, 8/8 (incluye el nuevo caso de secreto corto y el enmascarado de `RH_JWT_SECRET`).
- NOT RUN: `SesionE2ETests` (3), `RegistroE2ETests` (1) e integraciones PostgreSQL: la sesion no tiene `DB_URL`, `DB_USER`, `DB_PASSWORD` ni `CATALOGO_DB_TESTS`. NOT RUN: navegador real y Lighthouse de `/login`; el servidor no escucha en 8443.
- Limitacion declarada: sin estado en el servidor, el logout borra la cookie pero no revoca un token ya copiado; caduca a los 60 minutos. Queda medida por `SesionE2ETests.elTokenCopiadoAntesDelLogoutSigueValidoHastaExpirar` para decidir despues lista de revocacion o token opaco.
- Limitacion declarada: no hay limite de intentos ni bloqueo temporal por cuenta; un atacante puede probar contrasenas en serie. BCrypt coste 12 encarece cada intento, pero no sustituye una politica de intentos. Pendiente de decision antes de aceptar el hito.
- Corregido de la auditoria anterior: `HEAD` sobre rutas publicas ya no responde 401 (se declaro junto a `GET`); sigue pendiente comprobar cabeceras CSP, Referrer-Policy y Permissions-Policy.
- Tu turno reservado (sin implementar): `Cache-Control: no-store` en la respuesta de `/login` y su prueba. Pendiente: revision en navegador, E2E con PostgreSQL real, `APROBADA`, ficha y commit.

| Orden | Resultado y dependencias | Dedicacion estimada del estudiante con asistencia |
|---|---|---|
| 1 | HTTPS local, certificado y confianza verificados | 1–2 h |
| 2 | Registro BCrypt, login/JWT, logout y roles con CSRF; dividir en hitos de hasta 4 h | 5–7 h |
| 3 | Google OIDC, proveedor+sub y JWT propio; preparar cliente Google desde ahora | 2–4 h |
| 4 | Presentacion/CRUD, consentimiento, moderacion y auditoria en la misma transaccion; dividir en hitos de hasta 4 h | 6–9 h |
| 5 | Reporte JDBC/CSV y bitacora administrativa contrastados con SQL | 2–3 h |
| 6 | Revision integrada, JavaDoc/OpenAPI, OWASP, i18n y accesibilidad | 4–6 h |
| 7 | Cinco participantes, tres tareas, SUS y documentacion final; convocar desde ahora | 4–6 h |

[Inferencia] Total restante 24–37 h, con pruebas y defensa. No equivale a tiempo autonomo de IA ni garantiza capacidad de herramientas. Disponibilidad previa: 1–4 h/dia, miercoles sin disponibilidad. Hasta el 20 quedan 4–16 h contando hoy completo como maximo; hasta el 22, 6–24 h. La hora actual reduce lo disponible hoy. Incluso el extremo favorable deja margen nulo para incidencias antes de la entrega del 23. No se puede afirmar que el alcance completo estara listo.

Calendario condicionado: 16 HTTPS; 17 registro y primera parte de autenticacion; 18 no disponible; 19 completar autenticacion y comprobar permisos; 20 regresion y congelamiento de lo realmente aceptado. Los modulos 3–7 requieren disponibilidad adicional o cambio acordado de plazo/alcance. No trasladarlos silenciosamente al 21–22, reservados para cierre. Si se mantiene el plazo y la disponibilidad, presentar al docente las carencias de CRUD/Swagger (criterio 2), JDBC (3), Google/triggers (4) y reporte/usabilidad (5). La nota depende del docente; no asignar puntos inventados.

Recortes propuestos en orden: fidelidad visual, RF-12 opcional, extras de auditoria y refactorizaciones decorativas. Los Must permanecen comprometidos hasta una decision explicita. Revisar disponibilidad exacta y capacidad de convocar participantes antes de prometer fechas de cada modulo.

HTTPS en curso: perfil application-https.properties, comando scripts/iniciar-https.ps1 y guia docs/development/https-local.md. El alumno verifica SAN y vigencia como ejercicio. Arranque real pendiente de su entorno privado. La evaluacion docente aumenta a diagnostico y alternativas, con retroalimentacion y recomprobacion de conceptos mal explicados.

Continuacion mientras el estudiante investiga la defensa: agregado scripts/verificar-https.ps1 para catalogo ES/EN, Swagger/OpenAPI y rechazo de nombre incorrecto usando el certificado publico y curl con validacion TLS activa. PASS de sintaxis PowerShell y git diff --check. Comprobacion real BLOCKED: localhost.crt ausente de la ubicacion acordada y sin servidor observado en 8443. Las preguntas de defensa permanecen pendientes; no se exige responderlas para preparar estas comprobaciones. Sigue pendiente el arranque en el entorno configurado del estudiante.

Actualizacion HTTPS: tras el arranque del estudiante, Windows PowerShell 5.1 interrumpia el verificador al redirigir stderr del rechazo TLS esperado. Se sustituyo la redireccion del shell por --stderr de curl en ambas llamadas. [Verificado: powershell.exe -NoProfile -File scripts/verificar-https.ps1] PASS real, codigo 0: catalogo ES/EN, Swagger y OpenAPI con HTTP 200 y TLS validado; nombre incorrecto rechazado con codigo 60. Evidencias locales target/https/e38c8117cee84b43927b377534ae1e94. Se resuelve el bloqueo tecnico anterior. Pendientes confianza/recorrido HTTPS en navegador, ejercicio SAN/vigencia, defensa y aceptacion.

Cierre autorizado: el estudiante delego la evaluacion final y confirmo continuar. Se corrigio su uso de «segregacion de validacion»: la cobertura depende de las aserciones y la independencia entre pruebas es una propiedad distinta. Ficha etapa-01c.md. Preferencia docente vigente: dificultad adaptativa, con preguntas de diagnostico y defensa de alternativas tras respuestas correctas; explicar y volver a comprobar errores conceptuales. La autorizacion de HTTPS no acepta anticipadamente esa etapa.

Seguimiento de estabilizacion: recuperacion de pagina conserva filtros/idioma, claves singular/plural conectadas y GET de Swagger permitidos. README principal creado. Se agregaron tres pruebas de integracion; ejercicio de vacio EN reservado al estudiante. Compilacion de aplicacion y pruebas PASS con Maven Wrapper/JDK 25; ejecucion PostgreSQL BLOCKED por variables privadas ausentes en este proceso. Revision de navegador NOT RUN. Ninguna de estas pruebas nuevas se declara aprobada por compilar. Cierre, defensa y commit pendientes.

Actualizacion posterior: el estudiante ejecuto scripts/verificar.ps1 y aporto el sufijo 51e66be439874f20b3460077612bd9c0. [Verificado: XML de Surefire] 11 pruebas de catalogo + 2 de esquema + 1 de contexto, 0 fallos, 0 errores y 0 omitidas. Revisado validarEstadoVacioEn: consulta unica con marca + ausente, lang=en y aserciones de 200, contador cero y mensaje vacio. Ejercicio completado correctamente. git diff --check PASS. El bloqueo de ejecucion queda resuelto por esta evidencia de la terminal del estudiante; navegador y defensa siguen pendientes. No se repitio la suite.

## Decisiones vigentes (2026-11-12)

- Anteproyecto: `C:/Users/samuq/Downloads/formato_anteproyecto-PW.pdf`, leído completo.
- Rúbrica original: `C:/Users/samuq/Downloads/Proyecto PW/Rúbrica.png`, cinco criterios de un punto.
- Por confirmar con el docente: si Angular y MySQL son alternativas en la rúbrica; se conserva Thymeleaf/PostgreSQL del anteproyecto.
- D1–D7 confirmadas: omitir OneDrive/SharePoint; exportar CSV; fragmentos Thymeleaf; JOSE/Nimbus; recursos frontend locales; evidencia privada de autorización sin subida de archivos; CRUD con propiedad, moderación y retiro lógico.
- RF-12 (reenvío) sigue opcional. Editar rechazados no autoriza automáticamente reenviarlos.
- Disponibilidad declarada: 1–4 h/día, miércoles no disponible. Congelamiento objetivo: 20/11; cierre: 21–22/11; entrega: 23/11. Son metas, no garantías.
- E1A autorizada mediante «OK plan» y E1B mediante «Continua con la creación de la página web». El 12/11/2026 el estudiante autorizó expresamente commits de seguimiento del avance existente. Esto no acredita la aceptación final de E1B ni autoriza implementar mejoras propuestas.

## Estado por requisito

E1B aceptada expresamente el 14/11/2026. Criterios de rúbrica: 1 frontend; 2 backend; 3 datos; 4 seguridad; 5 i18n, accesibilidad, usabilidad y reporte.

| ID | Estado | Evidencia o pendiente | Rúbrica |
|---|---|---|---|
| RF-01, RF-02, RF-03, RF-04 | APROBADO | E1B aceptada el 14/11: catálogo, filtros combinables, paginación, detalle y búsqueda AJAX. Mejoras M4/M5 pendientes, sin ocultar sus límites. | 1, 2, 3 |
| RF-05 | EN VERIFICACIÓN | Registro aceptado; login/JWT/logout implementados, E2E anterior PASS; limites de seguridad y verificacion actual pendientes. | 4 |
| RF-06 | PENDIENTE | Dependencias presentes; Google OIDC no implementado. | 4 |
| RF-07, RF-08 | EN DESARROLLO | Entidades y V2; falta flujo de presentación y moderación. | 1, 2, 4 |
| RF-09 | EN DESARROLLO | Tabla/entidad de bitácora; sin triggers. | 3, 4 |
| RF-10 | PENDIENTE | Sin reporte JDBC ni CSV. | 3, 5 |
| RF-11, RF-12 | PENDIENTE | Sin mis propuestas ni reenvío; Should/Could respectivamente. | 1, 2 |
| RF-13 | PENDIENTE | Sin CRUD funcional documentado. | 2 |
| NF-01 | EN VERIFICACIÓN | E1B: bundles ES/EN, cookie de idioma y enlaces que conservan filtros; pruebas de vistas y fragmentos. | 5 |
| NF-02, NF-03 | EN VERIFICACIÓN | HTTPS/BCrypt/CSRF y JWT en cookie implementados; 401 API corregido. Permisos de negocio de modulos futuros y limites del token pendientes. | 4 |
| NF-04 | PENDIENTE | Falta auditoria OWASP completa documentada sobre los flujos finales. | 4 |
| NF-05 | EN DESARROLLO | JavaDoc de ping, DTO y contratos del catálogo; generación y cobertura completa pendientes. | 2 |
| NF-06 | EN DESARROLLO | 58 pruebas previas y 38 sin DB actuales verificadas; suite completa actual esperada 60, pendiente de ejecutar. Faltan pruebas de modulos futuros. | 2, 3 |
| NF-07 | EN DESARROLLO | E1B: etiquetas, foco, teclado y auditorías Lighthouse/axe verificadas; deberá revisarse cada nueva vista. No implica conformidad global. | 5 |
| NF-08, NF-09 | PENDIENTE | Faltan estudio real y documentación académica. | 5, Todos |

## E1A — evidencia y límites

[Verificado] Git inicial: `main`, HEAD `dd366f3`. Cambios previos conservados: movimientos de controladores, V1 modificada, V2 sin seguimiento y `.idea/` raíz sin seguimiento. Se conserva la rama actual para no mezclar una operación de Git con el trabajo previo.

[Verificado] Conexión configurada en IntelliJ: `reminishub_test`, PostgreSQL 18.6. Consulta de solo lectura: V1 y V2 exitosas, checksums -1039322172 y 809146767; ocho tablas de dominio; ningún trigger de usuario; cero proyectos y cero autores CUENTA_PROPIA sin usuario. El rol no es superusuario y no puede crear bases. No se guardan credenciales aquí.

[Verificado] Java/SQL diferían en DATO_FICTIO/DATO_FICTICIO. Se corrige el enum. `Proyecto.esFicticio` deja de participar en INSERT/UPDATE de JPA; el DEFAULT false pertenece a la base y los datos ficticios se cargarán con SQL controlado. Esto no sustituye la futura protección por trigger ni los permisos del rol.

V1 se conserva. La primera validación Flyway detectó V2 con checksum local 927336457 frente al aplicado 809146767. Se encontró la copia original en `C:/Users/samuq/Downloads/Proyecto PW/reminis-hub/src/main/resources/db/migration/V2__esquema.sql`: solo faltaban dos comentarios finales en la copia de trabajo. Se recuperó ese archivo original, sin cambios al SQL ejecutable ni uso de repair. V3 agrega el vínculo obligatorio de CUENTA_PROPIA y falla si encuentra registros incompatibles. Los triggers futuros deberán usar la siguiente versión disponible; los comentarios históricos de V2 que mencionan V3 no obligan a modificar una migración aplicada.

La prueba `EsquemaExistenteTests` valida checksums con Flyway sin ejecutar migrate; ensaya V3 sobre una tabla temporal que copia CHECK e identidad, sin copiar FK. Hace rollback y no modifica tablas ni secuencias del dominio. No demuestra integración completa JPA/CRUD ni auditoría.

Tu turno completado y revisado: el estudiante corrigió los nombres `setRol(RolUsuario)` y `setActivo(Boolean)` en `Usuario.java`, conservando sus cuerpos. La ficha de defensa se crea después de aceptar la etapa.

### Verificaciones

| Comprobación | Estado | Evidencia |
|---|---|---|
| Compilación Java 25 | PASS | Maven 3.10.0, `mvn -o -DskipTests compile`. |
| Consulta de historial/esquema | PASS | psql con `default_transaction_read_only=on`. |
| Checksums Flyway | PASS | Tras recuperar V2, `mvn -o -ntp -Dtest=EsquemaExistenteTests test`: 2 pruebas, 0 fallos, 0 errores, 0 omitidas. |
| Autorización PostgreSQL | PASS | `EsquemaExistenteTests`: enum válido, cuenta propia vinculada y rechazo SQLSTATE 23514 sin usuario. |
| Validación Hibernate contra esquema real | PASS | `ReminisHubApplicationTests`, Flyway desactivado y conexión Hikari de solo lectura; contexto iniciado con Hibernate 7.4.13.Final. |
| Aplicación persistente de V3 | PASS | Ejecución autorizada contra `reminishub_test`, Flyway target=3: validó las tres migraciones y aplicó una, pasando de v2 a v3. |
| Arranque tras V3 | PASS | `mvn -o -ntp -Dtest=ReminisHubApplicationTests test`, Flyway activo y ddl-auto=validate: 1 prueba, 0 fallos, 0 errores, 0 omitidas. |
| Revisión de espacios del diff | PASS | `git diff --check`. |

E1A: preguntas revisadas y aclaradas; el estudiante indicó continuar con la página web. Se creó `docs/defensa/etapa-01a.md`. Registrada posteriormente en el commit `8b02066`. Resultado técnico inicial: dos pruebas de esquema y una de contexto exitosas en ejecuciones separadas. No acredita CRUD, autenticación propia ni triggers.

Para repetir la comprobación sin migrar, desde `reminis-hub` en PowerShell, con Java 25 y `DB_URL`, `DB_USER`, `DB_PASSWORD`, `DEV_USER_PASSWORD` ya definidos en el proceso (las variables de IntelliJ no se heredan automáticamente):

```powershell
$env:E1A_DB_CHECKS = 'true'
$env:SPRING_FLYWAY_ENABLED = 'false'
$env:SPRING_DATASOURCE_HIKARI_READ_ONLY = 'true'
.\mvnw.cmd -o -ntp '-Dtest=EsquemaExistenteTests,ReminisHubApplicationTests' test
Remove-Item Env:E1A_DB_CHECKS, Env:SPRING_FLYWAY_ENABLED, Env:SPRING_DATASOURCE_HIKARI_READ_ONLY
```

Estos valores desactivan Flyway únicamente en ese proceso de prueba; no se cambió `application.properties`. La prueba directa de Flyway ejecuta validate, y la conexión JDBC directa solo crea objetos temporales. El primer intento offline encontró componentes de Surefire ausentes; se descargaron sin cambiar el POM. Los informes de ejecución se generan en `reminis-hub/target/surefire-reports` (excluidos de Git).

### Comprobaciones de comprensión realizadas en E1A

1. Explicar por qué V2 aplicada se conserva y la corrección usa V3.
2. Explicar por qué el nombre del enum debe coincidir con el valor SQL al usar EnumType.STRING.
3. Explicar por qué ocultar un campo en un formulario no sustituye una restricción SQL.
4. Distinguir validar Hibernate, validar Flyway y probar una regla de negocio.

## Preparación de usabilidad

Convocar al menos cinco personas reales desde el inicio. Cuando existan los flujos: buscar un proyecto, presentar uno y revisarlo como administrador con cuentas de prueba. Registrar por participante anónimo éxito, tiempo, incidencias, respuestas SUS y utilidad percibida; no inventar resultados. Falta acordar disponibilidad y preparar el instrumento completo en la etapa correspondiente.

## E1B — catálogo y detalle públicos

Autorización: «Continua con la creación de la página web»; el estudiante aprobó además cargar ejemplos ficticios. Se conserva el alcance local, Thymeleaf, Bootstrap 5.3.8 y jQuery 3.7.1. No se publicó un Site ni se incorporó otro framework. Referencia visual: sistema de diseño del ZIP Stitch, reconstruido con CSS y Bootstrap; marca ReminisHub, fuentes del sistema y recursos locales con licencias.

### Recorrido y decisiones

- `controllerMVC` renderiza catálogo, fragmento de resultados y detalle. `CatalogoApiController` ofrece GET `/api/v1/proyectos` y `/{id}`.
- `CatalogoService` valida parámetros, exige PUBLICADO, convierte entidades a DTO dentro de una transacción de lectura y omite información privada. No expone correos, cuentas, motivos de decisión ni evidencias.
- `ProyectoRepository` combina texto, categoría, año y tecnología. Seis resultados por página; orden año, semestre e ID descendentes; EXISTS evita duplicados por tecnologías. `%`, `_` y `!` se escapan para buscar texto literal.
- DTO: contrato público explícito. Transacción: mantiene disponibles las relaciones mientras se convierten a DTO, conservando open-in-view=false.
- Thymeleaf escapa contenido con th:text. Un proyecto inexistente o no publicado produce 404. Los autores se muestran solo con metadatos de autorización suficientes o como datos ficticios; esto no demuestra consentimiento real ni sustituye la futura revisión administrativa.
- jQuery solicita el fragmento HTML, espera 300 ms tras escribir, cancela solicitudes anteriores y conserva filtros en la URL y al cambiar idioma. Formulario y paginación siguen funcionando sin JavaScript.
- SecurityConfig permite únicamente GET públicos enumerados y mantiene CSRF. Las demás rutas se deniegan; no se ha implementado aún autenticación propia, registro ni botones que aparenten esos flujos.

### Datos reales de la demostración

V4 agrega siete categorías y 36 tecnologías. V5 agrega ocho proyectos PUBLICADO con es_ficticio=true, autores DATO_FICTICIO y cuenta de carga inactiva, sin contraseña. Ambos archivos ya aplicados: no editarlos; próxima versión V6. Los enlaces GitHub de esos proyectos son ilustrativos y pueden no existir; el detalle lo advierte. No son proyectos reales ni evidencia de usabilidad. Los triggers siguen pendientes.

### Verificación

| Comprobación | Estado | Evidencia |
|---|---|---|
| Compilación y suite completa | PASS | Java 25, `mvn -o -ntp test`: 10 pruebas, 0 fallos, 0 errores, 0 omitidas. Variables CATALOGO_DB_TESTS=true y E1A_DB_CHECKS=true. |
| Filtros, paginación y privacidad | PASS | CatalogoIntegrationTests contra PostgreSQL: filtros combinados, comodines literales, orden/desempate, páginas sin repetidos, 404 para tres estados privados. |
| Vistas ES/EN y seguridad de salida | PASS | Renderizado Thymeleaf, cookie de idioma, fragmento en inglés, escape de script, exclusión de evidencia privada y POST sin CSRF rechazado. |
| Ejercicio del estudiante | PASS | Agregó invalidarPaginasValorNegativo. Revisión: se agregó la barra inicial de la ruta y se retiró el cuerpo JSON innecesario; comprueba HTTP 400. |
| V4/V5 y esquema | PASS | Flyway aplicó ambas y validó las cinco migraciones; Hibernate validate pasó. |
| Búsqueda e idioma en navegador | PASS | Buscar Aula devuelve una ficha; cambiar a EN conserva el filtro y traduce la interfaz. |
| Diseño responsive y navegación completa | NOT RUN | Revisión parcial a 390 px: sin desbordamiento horizontal (scrollWidth=375). Falta completar recorrido de detalle, paginación y teclado; el clic de detalle no produjo navegación observable. No se acredita revisión completa. |
| Lighthouse y axe | NOT RUN | No se declara conformidad WCAG; auditoría formal pendiente. |

Las pruebas de catálogo crean fixtures en transacciones que se revierten; pueden avanzar secuencias de identidad. No borran proyectos ni reinician identificadores. Las pruebas no dependen de los ejemplos V5.

Para iniciar en IntelliJ: ejecutar `ReminisHubApplication` con sus variables DB_URL, DB_USER, DB_PASSWORD y DEV_USER_PASSWORD existentes; abrir `http://localhost:8080/proyectos` salvo puerto configurado distinto. Para ejecutar la suite en PowerShell, usar Java 25 y las mismas variables, establecer E1A_DB_CHECKS y CATALOGO_DB_TESTS en true y ejecutar `.\mvnw.cmd -o -ntp test` desde `reminis-hub`. Sin esas banderas, las pruebas de integración opcionales quedan omitidas.

Referencias técnicas consultadas: [Spring Security](https://docs.spring.io/spring-security/reference/7.0/servlet/authorization/authorize-http-requests.html), [locales MVC](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/localeresolver.html), [Jakarta Column](https://jakarta.ee/specifications/platform/11/apidocs/jakarta/persistence/Column.html), [Bootstrap](https://getbootstrap.com/docs/5.3/getting-started/download/), [jQuery](https://releases.jquery.com/). Las versiones efectivas de Boot 4.1.2 y Hibernate 7.4.13.Final se verificaron también durante las pruebas.

## Revisión de seguimiento — 12/11/2026

Alcance autorizado: revisar y registrar el trabajo existente, sin implementar nuevas mejoras. Un commit es un punto recuperable del código; no significa que la etapa esté aceptada o que toda la rúbrica esté satisfecha. Se conserva main y se incluyen los movimientos previos de controladores y las correcciones del estudiante. No se hizo push, rebase ni modificación de migraciones durante esta revisión.

### Historial del avance

| Commit | Mensaje | Alcance |
|---|---|---|
| `8b02066` | Corrección de las entidades y validación del esquema de la bd | V1 existente, V2 recuperada, V3, enum, setters, esFicticio y pruebas del esquema. |
| `f193465` | Creación del catálogo de proyectos con filtros, idiomas y pruebas | Backend de lectura, vistas, AJAX, ES/EN, recursos locales, V4/V5 y siete pruebas de catálogo. |

El commit documental que contiene esta sección se identifica mediante `git log -- docs/development/trazabilidad.md`; su propio hash no puede incluirse dentro de sí mismo. Los mensajes siguen las descripciones breves en español del historial existente.

### Contraste con la rúbrica original

[Verificado: Rúbrica.png, reinspeccionada; pom.xml, src y pruebas] No se asigna una nota ni un porcentaje de cumplimiento.

| Criterio (1 punto cada uno) | Avance demostrado | Pendiente para acreditarlo |
|---|---|---|
| Frontend | HTML/CSS, Bootstrap y jQuery locales; catálogo conectado con PostgreSQL. | Recorrido completo responsive/teclado; formularios y paneles; confirmar con docente la mención de Angular. |
| Backend | Java, API GET y DTO; siete pruebas de catálogo; JavaDoc parcial. | CRUD de escritura, generación de JavaDocs y Swagger utilizable: SecurityConfig deniega actualmente sus rutas. Tener springdoc en el POM no prueba cumplimiento. |
| Base de datos | PostgreSQL/JPA, cinco migraciones validadas; constraints y datos ficticios. | JDBC de reportes, SQL contrastado y prueba de instalación desde una base vacía; confirmar la mención de MySQL. JdbcTemplate en pruebas no sustituye el reporte. |
| Seguridad | Lectura pública limitada a PUBLICADO, DTO sin evidencia privada, escape HTML y rechazo por CSRF probado. | BCrypt, JWT, HTTPS, roles, Google, triggers y auditoría real. La configuración de usuario de desarrollo no implementa estos flujos. |
| i18n/accesibilidad/usabilidad/reporte | Interfaz del catálogo ES/EN; controles etiquetados y foco definido. | Lighthouse/axe, teclado completo, cinco participantes reales, SUS, reporte y documentación final. |

La base no tiene aún protección por triggers para transiciones, auditoría, actualización temporal ni inmutabilidad de es_ficticio. Son faltantes obligatorios ya planificados, no mejoras opcionales. Deben resolverse antes de considerar completos los flujos de escritura. No se afirma que la falta de navegación observada anteriormente sea un defecto confirmado del código: falta reproducirla con el servidor activo.

### Evidencia de esta revisión

| Verificación | Estado | Comando/evidencia y límite |
|---|---|---|
| Suite completa repetida | PASS | Maven local `-o -ntp test`, Java 25.0.5; CATALOGO_DB_TESTS=true y E1A_DB_CHECKS=true. 10 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS. |
| Historial aplicado | PASS | La suite valida cinco migraciones; esquema v5 actualizado, sin migraciones nuevas. No equivale a probar una instalación vacía. |
| Archivos y secretos conocidos | PASS | Comparación de los archivos candidatos con las dos contraseñas configuradas localmente: sin coincidencias, sin imprimir valores. Selección explícita de src y documentos; .idea y target excluidos. No sustituye un escáner exhaustivo de secretos. |
| Claves de idiomas | PASS | Compare-Object sobre claves de messages.properties/messages_en.properties: sin diferencias. No prueba calidad lingüística ni contenido aportado por usuarios. |
| Espacios y selección Git | PASS | git diff --check y git diff --cached --check; revisión de diff, stat y nombres antes del commit. |
| Navegador completo y accesibilidad | NOT RUN | Se conserva evidencia parcial de E1B; esta revisión no repitió navegador, Lighthouse ni axe. |
| Instalación en otra base vacía | NOT RUN | La conexión actual es reminishub_test ya migrada; no se crearon ni borraron bases. |
| Defensa y aceptación E1B | NOT RUN | Ejercicio revisado; quedan preguntas de cierre y aprobación. Sin ficha de defensa E1B todavía. |

Los informes XML de Surefire quedan en target/surefire-reports, fuera de Git. La suite usa PostgreSQL real con rollback de fixtures; no prueba todos los requisitos del sistema ni demuestra ausencia total de vulnerabilidades. El aviso de carga dinámica de Mockito no produjo fallos; su configuración futura debe revisarse si cambia el JDK.

### Mejoras propuestas — pendientes de aprobación

| ID | Prioridad y propuesta concreta | Archivos/impacto | Aceptación propuesta |
|---|---|---|---|
| M1 | Alta: agregar .gitignore en la raíz para IDE, secretos, certificados y temporales. | Nuevo archivo raíz; conserva archivos locales y reglas del módulo. | git check-ignore confirma protección de .idea raíz y ejemplos de secretos; src/docs siguen versionables. |
| M2 | Alta: comando PowerShell reproducible de verificación que active ambas banderas y compruebe omisiones. | Script local sin credenciales; no agrega dependencias. | Falla si falta configuración, Maven falla o se omiten suites esperadas; funciona con variables de entorno y muestra solo resultados seguros. |
| M3 | Media: cerrar QA del catálogo: detalle, páginas, vacío, ES/EN, sin JS, teclado, móvil/escritorio y accesibilidad. | Primero reproducir y documentar; cualquier corrección se presenta con su evidencia. | Recorridos y auditorías con PASS/FAIL y capturas; resolver la navegación no observada antes de aceptar E1B. |
| M4 | Baja: corregir singular/plural y definir respuesta para páginas fuera del total. | Bundles, resultados y contrato de paginación; no tocar migraciones. | Mostrar «1 proyecto encontrado»/«1 project found» y evitar «Página 101 de 2»; probar cero, uno, varios y página fuera de rango. |
| M5 | Media: completar JavaDoc y trasladar consultas de opciones desde controllerMVC al servicio. | Controlador/servicio; conservar contratos y evitar nuevas capas. | Controlador sin acceso directo a repositorios; pruebas actuales pasan y documentación generada revisada. |

M4 parte de observaciones concretas: los bundles usan siempre plural; CatalogoService admite pagina hasta 10000 sin contrastar el total, y el fragmento imprime pagina+1. M5 es una mejora de consistencia arquitectónica, no un fallo de seguridad demostrado. No se implementó ninguna de estas propuestas en esta revisión.

### Plazo y siguiente control

[Declarado] Entrega 23/11, congelamiento objetivo 20/11; 1–4 horas al día, miércoles no disponible. Desde el 13 al 20 quedan siete días disponibles (excluido el miércoles 18): 7–28 horas del estudiante. El 21–22 aporta 2–8 horas para cierre. No se cuenta como disponible el resto del día 12 ni se garantiza disponer de las cuatro horas máximas.

[Evaluación] Riesgo alto por autenticación, Google/HTTPS, escritura, triggers, reporte y validación humana aún pendientes. La asistencia acelera implementación, pero no sustituye comprensión, credenciales del proveedor ni cinco participantes reales. No se recorta ningún obligatorio. Si los hitos exceden la disponibilidad, corresponde negociar explícitamente alcance/plazo con el docente; primero reducir fidelidad visual y RF-12 opcional.

Siguiente control propuesto en ese momento: aprobar M1–M3, completar defensa E1B y cerrar su aceptación antes de autorizar E2. Confirmar con el docente Angular/MySQL y convocar participantes reales sigue siendo una tarea del estudiante.

## Avance M1–M3 — 13/11/2026

Autorizacion: respuesta «Sí» a M1–M3 y posterior «Continua con la elaboración proyecto, realizando la trazabilidad mediante git». Se conserva el permiso de commits de seguimiento, sin atribuir aceptacion final de E1B. M4/M5 siguen pendientes de aprobacion.

Commit `b8c7df5`: «Creación de la verificación de pruebas y protección de archivos locales». Las evidencias de M3 se registran en un commit documental posterior, identificable con `git log -- docs/evidencias`.

- M1 implementada: .gitignore raiz protege IDE, secretos, claves y temporales. `git check-ignore -v` confirma .idea, .env, configuracion local, claves y target; src/docs y .env.example siguen versionables. No borra archivos existentes.
- M2 implementada: scripts/verificar.ps1 activa las suites opcionales y verifica los XML de la ejecucion mediante un identificador unico. Rechaza omisiones, errores, suites faltantes y cantidades inferiores a las actuales; restaura las variables modificadas. Los valores privados se ocultan antes de escribir logs. Uso en scripts/README.md.
- M3 revisada parcialmente: detalle, filtros, paginacion, vacio ES/EN, teclado y dos anchos verificados. Lighthouse completo en dos vistas, con hallazgo de nombres accesibles pese a puntuacion 100. Axe independiente bloqueado por ChromeDriver ausente; prueba sin JavaScript pendiente. Evidencia y propuesta M6 en docs/evidencias/README.md. No se declara conformidad WCAG ni cierre de M3.

### Validacion del verificador

| Caso | Estado | Resultado |
|---|---|---|
| Variables de conexion ausentes | PASS | Rechaza antes de iniciar Maven; imprime nombres, no valores. |
| Maven simulado termina con error | PASS | Salida 1 del verificador. |
| Maven simulado exitoso sin informes nuevos | PASS | Rechaza resultados antiguos. |
| Suite ausente o pruebas omitidas | PASS | Ambos casos rechazados con salida 1; fixture local en target, sin acceso a DB. |
| Log con secreto simulado | PASS | El valor se sustituye por [OCULTO] antes de guardarse. |
| Ejecucion real con Maven local | PASS | 10 pruebas sin fallos/errores/omisiones. Sufijo 4c5c717d5d2b4b7d9b969b37e61b90f3. |
| Ejecucion final con Wrapper predeterminado | PASS | `scripts/verificar.ps1 -Offline`: 10 pruebas sin fallos/errores/omisiones; sufijo 0a417e897b0b4bee9b090fb788c341a0. Incluye proteccion del log. |

Durante la implementacion se corrigio un falso rechazo: Surefire agrega parentesis al nombre XML, pero guion al archivo. Ahora se identifica la suite desde el nombre de archivo documentado, sin aceptar informes antiguos. No se cambiaron pruebas Java ni migraciones.

Tu turno pendiente: ejecutar el comando documentado en tu terminal con las variables locales y comunicar solo el resumen PASS/FAIL. Defensa pendiente: por que comprobar omisiones ademas del codigo de salida; por que .gitignore no elimina secretos ya versionados; por que Lighthouse 100 no equivale a conformidad WCAG.

Fecha actual 13/11: hasta el congelamiento del 20 quedan seis dias completos disponibles despues de hoy (14–17 y 19–20), 6–24 horas, mas lo que realmente quede disponible hoy; reservar 21–22 para cierre. Se conserva el riesgo alto y no se recorta alcance obligatorio.

## Incidencia del verificador — revision cerrada el 14/11/2026

[Verificado: Surefire] El estudiante ejecuto el comando y reporto 10 errores el 13/11 a las 17:35. Los tres informes con sufijo `a99061e1fea7431c88455ba860e79d05` muestran 7+2+1 errores. La causa de arranque es `IllegalArgumentException: 'url' must start with "jdbc"`; la prueba JDBC directa falla tambien con `No suitable driver`. No son diez fallos independientes del catalogo: la configuracion impide establecer la conexion y arrancar los contextos. No se imprimio el valor privado de conexion ni se supone como lo introdujo el estudiante.

[Verificado: scripts/verificar.ps1] Habia ademas un defecto del verificador: rechazaba el codigo no cero de Maven antes de leer XML y mostrar contadores. Se corrige para resumir todas las suites disponibles, conservar un resumen.txt y devolver FAIL cuando corresponde. Se valida previamente una URL PostgreSQL con host y base, sin asignaciones, espacios, variables concatenadas ni credenciales incorporadas. Las variables de la terminal del estudiante no se modificaron desde Codex; scripts/README.md contiene los comandos no secretos para configurarlas correctamente.

| Comprobacion | Estado | Evidencia |
|---|---|---|
| Regresion del verificador | PASS | `scripts/probar-verificador.ps1`: siete escenarios sinteticos, sin PostgreSQL. Rechaza URL invalida, Maven fallido, informes ausentes, suite ausente, omisiones y diez errores; acepta ejecucion correcta. Comprueba ocultacion del secreto simulado en salida/log. |
| Suite real tras correccion | PASS | `scripts/verificar.ps1 -Offline`, configuracion existente de IntelliJ cargada solo en el proceso de verificacion: 10 pruebas, 0 fallos, 0 errores, 0 omitidas; Maven=0. Sufijo `695dbe14b8324c5a9e686996a0c3c0ec`. |
| Servidor y HTTP real | PASS | Arranque en 127.0.0.1:8081 y 14 solicitudes con Invoke-WebRequest -SkipHttpErrorCheck; detalle abajo. Servidor detenido al finalizar. |
| Nueva revision visual/auditoria | NOT RUN | No hubo cambios de HTML/CSS/JS en esta correccion. Se conserva la evidencia visual y Lighthouse del 13/11, con hallazgo M6 pendiente; axe independiente y prueba sin JavaScript siguen pendientes. |
| Nueva ejecucion en la terminal del estudiante | NOT RUN | Falta repetir con DB_URL corregida en su propia terminal. El PASS de Codex no certifica ese entorno. |

HTTP: GET `/api/v1/proyectos` y `?pagina=1` entregaron 6+2 registros sin IDs repetidos; GET `/proyectos/8` devolvio detalle con aviso ficticio; GET `/proyectos?lang=es` y `?lang=en` devolvieron textos esperados; GET `/catalogo/resultados?q=SinCoincidenciaQA` devolvio estado vacio sin cabecera completa; GET `/api/v1/proyectos?categoria=WEB&anio=2026&q=Aula` devolvio un resultado. Pagina -1: 400; ID inexistente: 404; POST sin CSRF: 403. CSS, JS, jQuery y Bootstrap locales: 200. Cabecera X-Content-Type-Options: nosniff. No se crearon proyectos ni se modificaron migraciones.

Ejercicio del estudiante recibido y revisado: su salida permitio detectar esta incidencia real. Pendiente que vuelva a ejecutar y comparta solo RESUMEN/PASS/FAIL. No hace falta habilitar -X ni compartir credenciales. El commit de esta correccion se identifica con `git log -- scripts/verificar.ps1`.

Al 14/11 quedan cinco dias completos disponibles despues de hoy hasta el 20 (15–17 y 19–20), 5–20 horas mas el tiempo disponible hoy. El riesgo alto de los requisitos aun pendientes se mantiene; esta correccion no constituye aceptacion de E1B ni implementacion de autenticacion/CRUD/reporte.

## Seguimiento del 14/11/2026 — M6 y defensa E1B

[Declarado por el estudiante] Confirma que el codigo corregido funciona correctamente en su terminal. Se da por recibido su ejercicio de ejecucion; no se inventa un nuevo informe Surefire de su entorno. La correccion previa esta en 6efdd8f.

[Verificado: respuestas del estudiante] Defensa conceptual satisfactoria: filtrar PUBLICADO en servidor restringe acceso; DTO limita el contrato expuesto; las pruebas de modulos existentes no verifican funcionalidades aun ausentes. Se preciso que DTO no sustituye autorizacion y que auditoria automatica tampoco sustituye revision manual.

[Verificado: fragments/layout.html] M6 expresamente aprobada e implementada: nombre accesible derivado del contenido de marca y nombres ES/EN que incluyen las siglas visibles. Se aprobo por separado role="group" para el selector de idiomas. No se modificaron migraciones ni dependencias de la aplicacion.

[Verificado: scripts/verificar.ps1 -Offline] Nueva ejecucion real: 10 pruebas, 0 fallos, 0 errores, 0 omitidas; sufijo 47fb8b52327c499991743a31735bdff5. Ejecutada tras M6 inicial; el role anadido despues se verifico mediante WebDriver sobre las cuatro vistas finales.

[Verificado: scripts/auditar-catalogo.cjs] Axe 4.14.0 con Edge WebDriver: cero infracciones en catalogo/detalle ES/EN, incluida regla experimental de coincidencia del nombre. PASS de nombres accesibles y recorrido sin JavaScript. La limitacion anterior de ChromeDriver queda resuelta utilizando Edge WebDriver. Evidencia completa en docs/evidencias; se conservan revisiones incomplete de contraste, sin afirmar conformidad WCAG.

Estado: E1B sigue EN VERIFICACION hasta aceptacion explicita. Defensa respondida; ficha final pendiente de aceptacion conforme a AGENTS.md. M4/M5 siguen sin aprobacion. La correccion M6 no implementa autenticacion, CRUD privado, auditoria por triggers ni reportes. El siguiente bloque funcional debe abordar seguridad local/HTTPS y autenticacion, dividido para mantener el contrato docente y las pruebas. No se inicia codigo de E2 en este seguimiento.

[Verificado: lighthouse-catalogo-es.json y lighthouse-detalle-en.json] Lighthouse final del 14/11: accesibilidad 100 y coincidencia de nombre aprobada en ambas vistas, sin auditorias binarias fallidas. Primer intento bloqueado por puerto dinamico de Edge; reintento exitoso con puerto fijo 9222. No se convierte este resultado en una declaracion global de conformidad.

## Cierre E1B y plan siguiente — 14/11/2026

[Declarado por el estudiante] E1B APROBADA. Solicita plan preciso de una etapa superior, sin modificaciones grandes, con 6% de limite semanal restante. Se conserva autorizacion de commits locales; no de push. La ficha aceptada es docs/defensa/etapa-01b.md. Esta seccion sustituye el estado historico de aceptacion pendiente, no elimina los limites de las evidencias.

[Verificado: git] Cierre tecnico registrado en `74be6a4`, «Corrección de los nombres accesibles y verificación del catálogo». El commit documental siguiente incorpora aceptacion, ficha y plan; localizarlo con `git log -- docs/defensa/etapa-01b.md`. No se repitio la suite Java en el cierre documental: no hubo cambios de aplicacion desde las verificaciones registradas.

### E2 propuesta: identidad y seguridad de acceso

Objetivo: registro e inicio local, JWT propio en cookie segura, logout y Google OIDC, manteniendo catalogo publico. RF-05/RF-06 y avance NF-02/NF-03; criterio de seguridad y aportes a frontend/backend. No incluye todavia CRUD, moderacion o reporte. Plan propuesto, NO implementado ni autorizado para ejecucion por esta aprobacion de E1B.

[Verificado: pom.xml, SecurityConfig, Usuario e IdentidadExterna] Existen Boot 4.1.2, Java 25, starters Security/OAuth2/resource-server, usuario con passwordHash/rol/activo e identidad con proveedor/subject. SecurityConfig permite GET publicos y deniega el resto; no hay autenticacion propia. No hace falta partir de otro stack. [Por confirmar antes de programar] Versiones transitivas reales y compatibilidad de thymeleaf-extras-springsecurity6; verificar documentacion oficial de Security/JOSE y no agregar o actualizar dependencias por intuicion.

Dividir E2 en hitos de hasta unas cuatro horas del estudiante; si alguno excede ese limite, subdividir antes de programar. No son estimaciones garantizadas de tiempo de IA.

| Hito | Cambios previstos | Pruebas y salida exigida |
|---|---|---|
| E2A: HTTPS local | Perfil de configuracion y guia/script local de certificado; secretos y keystore fuera de Git. Sin instalar confianza global automaticamente. | Arranque HTTPS; certificado y SAN para hostname elegido; recursos locales y catalogo ES/EN funcionan. No usar omitir validacion TLS como prueba de confianza. |
| E2B: registro local | UsuarioRepository, servicio transaccional, DTO/formulario mutable, controlador y vista ES/EN; BCrypt; rol COLABORADOR fijado en servidor. | Registro valido, duplicado y carrera de unicidad, datos invalidos, hash distinto del texto, rechazo de rol/activo enviados por cliente, CSRF y rollback PostgreSQL. Revisar politica de correo contra unicidad actual; si exige esquema, proponer nueva migracion. |
| E2C: login/JWT/logout | SecurityConfig y componentes de autenticacion; contratos de login y usuario actual; cookie HttpOnly/Secure, SameSite y vigencia documentados; logout por POST con CSRF. | Login correcto/incorrecto/inactivo; JWT alterado/caducado; API 401/403; redireccion web; cookie y CSRF en formularios/AJAX; logout. Precisar que borrar cookie no revoca por si solo un JWT robado: decidir politica y probarla antes de aceptar. |
| E2D: Google OIDC | Repositorio de identidad, flujo Spring OAuth2 y emision del mismo JWT propio. | Proveedor+sub unico; rechazo de vinculacion automatica solo por correo; state/redirect, errores del proveedor, usuario inactivo y flujo real de navegador. Pruebas simuladas no sustituyen login real con Google. |

Archivos: dentro de reminis-hub/src/main/java/dev/reminishub/{config,service,repository,dto,controller}, templates y bundles ES/EN existentes; pruebas en src/test/java; documentacion y scripts locales. No tocar migraciones aplicadas, recursos visuales ni datos ficticios. pom.xml solo si una necesidad concreta y compatibilidad verificadas justifican aprobacion adicional.

Dependencias externas: PostgreSQL local disponible; certificado/clave privados; cliente Google y URI exacta de retorno admitida por proveedor, comprobada antes de E2D. Credenciales solo en configuracion local, nunca en chat. Preparar estas condiciones temprano; si falta Google, marcar BLOCKED ese flujo, no declarar RF-06 completo. Revisar base aislada disponible antes de pruebas con escrituras.

Contenido docente: HTTPS protege transporte; BCrypt protege contrasenas almacenadas; JWT autentica; CSRF protege peticiones basadas en cookies; autorizacion decide permisos. Alternativa descartada: guardar tokens en localStorage o desactivar CSRF. Tu turno por hito: comprobar certificado; completar validacion pequena; escribir prueba de acceso sin autenticar; explicar/pruebar identidad proveedor+sub. Reservar piezas antes de implementar, revisar intento, cerrar con 3–5 preguntas y ficha tras aceptacion.

Aceptacion: PASS de casos del hito, regresion del catalogo, diff sin secretos y defensa comprendida; NOT RUN/BLOCKED explicitos no se cuentan como PASS. Conservar CSRF y autorizacion en servidor. Revisar APIs oficiales al implementar, no asumir que este plan verifica compatibilidad tecnica futura. Cada hito requiere OK plan, aceptacion y transicion conforme a AGENTS.md.

### Orden posterior y control del plazo

1. E3: presentacion/CRUD con propiedad, autores y consentimiento; moderacion y Mis propuestas. Incorporar auditoria de altas/cambios de estado junto a las primeras escrituras: trigger con actor autenticado en la misma transaccion/conexion; rollback y reutilizacion de conexiones probados.
2. E4: consultas JDBC, reporte/CSV contrastados con SQL, bitacora administrativa, JavaDoc/OpenAPI y revision OWASP aplicable.
3. E5: integracion, accesibilidad de nuevas vistas, tres tareas reales con al menos cinco participantes, SUS/utilidad y documentacion final. Convocar participantes ahora; ejecutar las tareas cuando existan los flujos. No inventar resultados.

[Declarado] Entrega 23/11, congelamiento deseado 20/11, 1–4 h/dia salvo miercoles. Al 14/11 quedan cinco dias completos disponibles hasta el 20 despues de hoy (15–17, 19–20), mas lo disponible hoy; 21–22 para cierre. [Evaluacion] Riesgo alto: el limite semanal puede impedir el calendario; no se conoce su fecha de renovacion ni se promete completar con 6%. No recortar autenticacion/Google, CRUD, auditoria, reporte, i18n o evaluacion humana. Si no caben, negociar plazo/alcance con docente antes de presentar un entregable incompleto como conforme. M4/M5 continuan propuestas sin aprobar; no son prerrequisito para iniciar E2A.

### Retomar con poco contexto

Leer AGENTS.md, esta seccion, scripts/README.md y git status/log. Conservar codigo y evidencias aprobados; no repetir diagnostico ni auditorias sin cambios relevantes. Ejecutar un hito a la vez, una compilacion a la vez, sin agentes innecesarios. Registrar resultados resumidos y commit por unidad verificada con el estilo del estudiante. Primera accion futura: aprobar el plan E2A y verificar requisitos locales de HTTPS; no iniciar E2 completa de una vez. En esta sesion solo se cierra E1B y se deja este plan.
