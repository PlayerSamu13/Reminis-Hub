# Verificacion local

Desde la raiz, en PowerShell, con Java 25 y PostgreSQL iniciados:

```powershell
.\scripts\verificar.ps1 -Offline
```

Configura previamente `DB_URL`, `DB_USER`, `DB_PASSWORD` y `DEV_USER_PASSWORD` en el proceso. Las variables de la configuracion de ejecucion de IntelliJ no se heredan automaticamente en su terminal. No pegues secretos en el chat ni los escribas en este repositorio. El script no importa configuraciones privadas del IDE.

Para la base de pruebas de este proyecto, configura los dos valores no secretos por separado:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/reminishub_test'
$env:DB_USER = 'web_user'
```

El valor de DB_URL no incluye el texto `DB_URL=`, comillas literales ni la cadena de variables separadas por punto y coma de IntelliJ. Conserva las contrasenas reales ya configuradas en el proceso. No uses valores de ejemplo como contrasenas. Esta configuracion solo afecta a la terminal actual.

Se usa `reminis-hub/mvnw.cmd`. Si tienes otra instalacion compatible de Maven, puedes indicar su ruta:

```powershell
.\scripts\verificar.ps1 -Offline -MavenCommand 'C:\ruta\a\maven\bin\mvn.cmd'
```

`-Offline` requiere dependencias ya descargadas. Omitelo si necesitas descargarlas. No desactives globalmente la politica de ejecucion de PowerShell: si impide ejecutar el script, informa del mensaje sin datos privados.

El comando activa las dos suites opcionales, ejecuta Maven y lee solo los informes de esa ejecucion, identificados con un GUID (identificador unico). Exige las tres suites actuales con al menos 7, 2 y 1 pruebas, y rechaza cualquier fallo, error u omision en los informes generados. Los minimos deben revisarse al reorganizar las pruebas. No demuestra requisitos sin pruebas.

El codigo de salida es 0 para PASS y 1 para FAIL. Aunque Maven falle, se resumen todas las suites con informes nuevos y se escribe `resumen.txt` junto al log; si no hay informes se indica NOT RUN, sin inventar contadores. Los valores anteriores de las banderas de entorno se restauran al terminar. Los logs quedan en `reminis-hub/target/verificacion/<identificador>/maven.log`; se ocultan los valores de las cuatro variables privadas antes de escribirlos. No compartas logs completos sin revisarlos: otras configuraciones futuras pueden contener datos sensibles.

La suite conecta a la base configurada; usa exclusivamente una base de desarrollo/pruebas. El arranque de Spring ejecuta Flyway: si agregas migraciones pendientes, puede aplicarlas. Actualmente V1-V5 ya estan aplicadas. Los fixtures del catalogo se revierten, pero las secuencias pueden avanzar. No se ejecuta clean, repair ni borrado de bases.

Los controles negativos del verificador se probaron con un comando Maven simulado en target (sin conectar a PostgreSQL): salida no cero, ausencia de informes nuevos, suite faltante y pruebas omitidas. Estos controles comprueban el script, no la aplicacion.

Para repetir las siete regresiones del verificador, sin credenciales reales ni PostgreSQL:

```powershell
.\scripts\probar-verificador.ps1
```

Incluyen URL invalida, error de Maven, informes ausentes, suite ausente, pruebas omitidas, diez errores con diagnostico y ejecucion correcta. Los archivos sinteticos quedan en target, excluidos de Git; llevan identificadores distintos y no se confunden con una ejecucion real. Las variables de conexion previas se restauran al terminar.

Referencia: [opciones oficiales de Surefire](https://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html), propiedad `surefire.reportNameSuffix`.
