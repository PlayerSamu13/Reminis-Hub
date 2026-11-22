# ReminisHub

Repositorio académico de proyectos. El catálogo público permite buscar, filtrar, paginar y consultar detalles en español, inglés y esperanto; el registro local crea cuentas de colaborador con BCrypt sobre HTTPS y el login emite un JWT propio en cookie HttpOnly. Los colaboradores pueden presentar, editar y retirar propuestas. Los administradores pueden moderarlas, consultar la bitácora y exportar el reporte CSV. El perfil Google habilita el acceso OIDC cuando se configuran sus credenciales. Consulta la [guía de operación](docs/development/operacion-propuestas.md).

## Entorno local

Se requiere JDK 25, PostgreSQL 18 y las dependencias Maven disponibles. El proyecto utiliza Spring Boot 4.1.2, Thymeleaf, Bootstrap y jQuery locales.

Configura JAVA_HOME con tu JDK y agrega su carpeta bin al PATH del proceso. Configura DB_URL, DB_USER, DB_PASSWORD y RH_JWT_SECRET en la terminal o en la ejecución de IntelliJ. RH_JWT_SECRET firma el JWT propio: al menos 32 bytes UTF-8, distinta en cada entorno y fuera de Git. La app no arranca sin ella. Guarda las contraseñas fuera de Git.

La plantilla reminis-hub/.env.example documenta variables. Copiarla a .env no hace que Spring las cargue automáticamente. La terminal no hereda necesariamente la configuración de ejecución de IntelliJ.

Para las pruebas autorizadas utiliza jdbc:postgresql://localhost:5432/reminishub_test. El estudiante identifica reminishub_dev como producción; no ejecutar pruebas ni migraciones de ensayo sobre ella.

## Arranque y verificación

El registro local exige HTTPS, así que el arranque de referencia es:

```powershell
.\scripts\iniciar-https.ps1
```

El script activa el perfil `https` y exige `DB_URL`, `DB_USER`, `DB_PASSWORD`, `RH_JWT_SECRET`, `RH_TLS_KEYSTORE` y `RH_TLS_PASSWORD`; también rechaza cualquier base distinta de `jdbc:postgresql://localhost:5432/reminishub_test`. Abre https://localhost:8443/proyectos. La documentación está en /swagger-ui/index.html y /v3/api-docs, y el procedimiento del certificado en [https-local](docs/development/https-local.md).

`GET /registro` redirige a `https://localhost:8443/registro`. Si arrancas con `.\mvnw.cmd spring-boot:run` sin perfil, el catálogo responde en http://localhost:8080/proyectos pero el formulario de registro queda inaccesible porque el puerto 8443 no escucha; no uses ese arranque para demostrar el registro. El arranque ejecuta Flyway y puede aplicar migraciones pendientes; verifica previamente la base seleccionada.

Para ejecutar pruebas desde la raíz sigue [scripts/README.md](scripts/README.md). El comando es `./scripts/verificar.ps1 -Offline` cuando las dependencias ya están descargadas. Con `CATALOGO_DB_TESTS=true` se activan las suites PostgreSQL, incluida la verificación de extremo a extremo del registro. Las pruebas deben usar exclusivamente la base autorizada.

Consulta el estado y las limitaciones en [trazabilidad](docs/development/trazabilidad.md) y las comprobaciones de interfaz en [evidencias](docs/evidencias/README.md).
### Acceso opcional con Google en HTTPS local

En Google Cloud configura un cliente OAuth de tipo aplicación web. Registra exactamente `https://localhost:8443/login/oauth2/code/google` como URI de redirección autorizada. Si la pantalla de consentimiento está en pruebas, añade la cuenta de demostración como usuario de prueba.

Configura `RH_GOOGLE_CLIENT_ID` y `RH_GOOGLE_CLIENT_SECRET` en la terminal privada que ya contiene las variables de HTTPS y PostgreSQL. Desde la raíz ejecuta `.\scripts\iniciar-https.ps1 -Google`. En `/login` selecciona «Continuar con Google». Al volver al catálogo, comprueba la identidad en `/api/v1/sesion`, cierra la sesión y comprueba que esa ruta devuelve 401. Conserva como evidencia el resultado y omite valores de cookies, códigos y secretos.
