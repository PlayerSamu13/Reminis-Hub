# Verificacion local

Desde la raiz, en PowerShell, con Java 25 y PostgreSQL iniciados:

```powershell
.\scripts\verificar.ps1 -Offline
```

Configura previamente `DB_URL`, `DB_USER`, `DB_PASSWORD` y `DEV_USER_PASSWORD` en el proceso. Las variables de la configuracion de ejecucion de IntelliJ no se heredan automaticamente en su terminal. No pegues secretos en el chat ni los escribas en este repositorio. El script no importa configuraciones privadas del IDE.

Se usa `reminis-hub/mvnw.cmd`. Si tienes otra instalacion compatible de Maven, puedes indicar su ruta:

```powershell
.\scripts\verificar.ps1 -Offline -MavenCommand 'C:\ruta\a\maven\bin\mvn.cmd'
```

`-Offline` requiere dependencias ya descargadas. Omitelo si necesitas descargarlas. No desactives globalmente la politica de ejecucion de PowerShell: si impide ejecutar el script, informa del mensaje sin datos privados.

El comando activa las dos suites opcionales, ejecuta Maven y lee solo los informes de esa ejecucion, identificados con un GUID (identificador unico). Exige las tres suites actuales con al menos 7, 2 y 1 pruebas, y rechaza cualquier fallo, error u omision en los informes generados. Los minimos deben revisarse al reorganizar las pruebas. No demuestra requisitos sin pruebas.

El codigo de salida es 0 para PASS y 1 para FAIL. Los valores anteriores de las banderas de entorno se restauran al terminar. Los logs quedan en `reminis-hub/target/verificacion/<identificador>/maven.log`; se ocultan los valores de las cuatro variables privadas antes de escribirlos. No compartas logs completos sin revisarlos: otras configuraciones futuras pueden contener datos sensibles.

La suite conecta a la base configurada; usa exclusivamente una base de desarrollo/pruebas. El arranque de Spring ejecuta Flyway: si agregas migraciones pendientes, puede aplicarlas. Actualmente V1-V5 ya estan aplicadas. Los fixtures del catalogo se revierten, pero las secuencias pueden avanzar. No se ejecuta clean, repair ni borrado de bases.

Los controles negativos del verificador se probaron con un comando Maven simulado en target (sin conectar a PostgreSQL): salida no cero, ausencia de informes nuevos, suite faltante y pruebas omitidas. Estos controles comprueban el script, no la aplicacion.

Referencia: [opciones oficiales de Surefire](https://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html), propiedad `surefire.reportNameSuffix`.
