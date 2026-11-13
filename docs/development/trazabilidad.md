# ReminisHub — trazabilidad

## Decisiones vigentes (2026-11-12)

- Anteproyecto: `C:/Users/samuq/Downloads/formato_anteproyecto-PW.pdf`, leído completo.
- Rúbrica original: `C:/Users/samuq/Downloads/Proyecto PW/Rúbrica.png`, cinco criterios de un punto.
- Por confirmar con el docente: si Angular y MySQL son alternativas en la rúbrica; se conserva Thymeleaf/PostgreSQL del anteproyecto.
- D1–D7 confirmadas: omitir OneDrive/SharePoint; exportar CSV; fragmentos Thymeleaf; JOSE/Nimbus; recursos frontend locales; evidencia privada de autorización sin subida de archivos; CRUD con propiedad, moderación y retiro lógico.
- RF-12 (reenvío) sigue opcional. Editar rechazados no autoriza automáticamente reenviarlos.
- Disponibilidad declarada: 1–4 h/día, miércoles no disponible. Congelamiento objetivo: 20/11; cierre: 21–22/11; entrega: 23/11. Son metas, no garantías.
- E1A autorizada mediante «OK plan» y E1B mediante «Continua con la creación de la página web». El 12/11/2026 el estudiante autorizó expresamente commits de seguimiento del avance existente. Esto no acredita la aceptación final de E1B ni autoriza implementar mejoras propuestas.

## Estado por requisito

Ningún requisito está APROBADO. Criterios de rúbrica: 1 frontend; 2 backend; 3 datos; 4 seguridad; 5 i18n, accesibilidad, usabilidad y reporte.

| ID | Estado | Evidencia o pendiente | Rúbrica |
|---|---|---|---|
| RF-01, RF-02, RF-03, RF-04 | EN VERIFICACIÓN | E1B: catálogo, filtros combinables, paginación, detalle y búsqueda AJAX; pendiente aceptación humana. | 1, 2, 3 |
| RF-05, RF-06 | PENDIENTE | Dependencias presentes, sin autenticación propia. | 4 |
| RF-07, RF-08 | EN DESARROLLO | Entidades y V2; falta flujo de presentación y moderación. | 1, 2, 4 |
| RF-09 | EN DESARROLLO | Tabla/entidad de bitácora; sin triggers. | 3, 4 |
| RF-10 | PENDIENTE | Sin reporte JDBC ni CSV. | 3, 5 |
| RF-11, RF-12 | PENDIENTE | Sin mis propuestas ni reenvío; Should/Could respectivamente. | 1, 2 |
| RF-13 | PENDIENTE | Sin CRUD funcional documentado. | 2 |
| NF-01 | EN VERIFICACIÓN | E1B: bundles ES/EN, cookie de idioma y enlaces que conservan filtros; pruebas de vistas y fragmentos. | 5 |
| NF-02, NF-03, NF-04 | PENDIENTE | Sin evidencia de JWT/cookies, HTTPS, permisos de negocio u OWASP. | 4 |
| NF-05 | EN DESARROLLO | JavaDoc de ping, DTO y contratos del catálogo; generación y cobertura completa pendientes. | 2 |
| NF-06 | EN DESARROLLO | 10 pruebas: 7 catálogo, 2 esquema y 1 contexto; faltan pruebas de los flujos aún no implementados. | 2, 3 |
| NF-07 | EN DESARROLLO | E1B: HTML semántico, etiquetas, foco, salto al contenido y movimiento reducido; auditorías Lighthouse/axe pendientes. | 5 |
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
