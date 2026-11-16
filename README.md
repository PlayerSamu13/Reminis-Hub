# ReminisHub

Repositorio académico de proyectos. El catálogo público permite buscar, filtrar, paginar y consultar detalles en español e inglés. Autenticación propia, CRUD privado, moderación, auditoría y reportes siguen pendientes.

## Entorno local

Se requiere JDK 25, PostgreSQL 18 y las dependencias Maven disponibles. El proyecto utiliza Spring Boot 4.1.2, Thymeleaf, Bootstrap y jQuery locales.

Configura JAVA_HOME con tu JDK y agrega su carpeta bin al PATH del proceso. Configura DB_URL, DB_USER, DB_PASSWORD y DEV_USER_PASSWORD en la terminal o en la ejecución de IntelliJ. DEV_USER_PASSWORD corresponde al usuario provisional de desarrollo. Guarda las contraseñas fuera de Git.

La plantilla reminis-hub/.env.example documenta variables. Copiarla a .env no hace que Spring las cargue automáticamente. La terminal no hereda necesariamente la configuración de ejecución de IntelliJ.

Para las pruebas autorizadas utiliza jdbc:postgresql://localhost:5432/reminishub_test. El estudiante identifica reminishub_dev como producción; no ejecutar pruebas ni migraciones de ensayo sobre ella.

## Arranque y verificación

Desde la raíz, con variables configuradas para la base de pruebas autorizada:

```powershell
Set-Location .\reminis-hub
.\mvnw.cmd spring-boot:run
```

Abre http://localhost:8080/proyectos. La documentación está en /swagger-ui/index.html y /v3/api-docs. El arranque ejecuta Flyway y puede aplicar migraciones pendientes; verifica previamente la base seleccionada.

Para ejecutar pruebas desde la raíz sigue [scripts/README.md](scripts/README.md). El comando es `./scripts/verificar.ps1 -Offline` cuando las dependencias ya están descargadas. Activa las suites PostgreSQL y resume errores y omisiones. Las pruebas deben usar exclusivamente la base autorizada.

Consulta el estado y las limitaciones en [trazabilidad](docs/development/trazabilidad.md) y las comprobaciones de interfaz en [evidencias](docs/evidencias/README.md).
