# HTTPS local — etapa en verificacion

Perfil preparado para Spring Boot 4.1.2. HTTPS cifra el transporte. El cliente debe verificar confianza, vigencia y nombre del servidor. PKCS12 contiene la clave privada y el certificado; el archivo CRT exportado contiene solo el certificado publico.

## Preparacion en PowerShell

Con JAVA_HOME apuntando al JDK 25, crea una carpeta privada fuera del repositorio:

```powershell
$tlsFolder = Join-Path $env:LOCALAPPDATA 'ReminisHub\tls'
New-Item -ItemType Directory -Path $tlsFolder -Force | Out-Null
$tlsStore = Join-Path $tlsFolder 'localhost.p12'
if (Test-Path $tlsStore) { throw 'Ya existe el certificado. Revisalo antes de reemplazarlo.' }
& "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -alias reminishub-local -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -storetype PKCS12 -keystore $tlsStore -validity 30 -dname 'CN=localhost,OU=Desarrollo,O=ReminisHub' -ext 'SAN=dns:localhost,ip:127.0.0.1' -ext 'EKU=serverAuth' -ext 'KU=digitalSignature,keyEncipherment' -ext 'BC=ca:false'
```

Keytool solicita la contrasena de forma interactiva. Usa una propia y guardala fuera del repositorio. Detente ante un codigo de salida distinto de cero.

Configura el mismo secreto para el proceso de Spring sin escribirlo en el historial:

```powershell
$tlsSecure = Read-Host 'Contrasena del PKCS12' -AsSecureString
$env:RH_TLS_PASSWORD = [System.Net.NetworkCredential]::new('', $tlsSecure).Password
$env:RH_TLS_KEYSTORE = ([Uri]$tlsStore).AbsoluteUri
& "$env:JAVA_HOME\bin\keytool.exe" -exportcert -rfc -alias reminishub-local -keystore $tlsStore -storepass:env RH_TLS_PASSWORD -file (Join-Path $tlsFolder 'localhost.crt')
& "$env:JAVA_HOME\bin\keytool.exe" -list -v -alias reminishub-local -keystore $tlsStore -storepass:env RH_TLS_PASSWORD
```

**Tu turno:** identifica SAN, fechas de vigencia y huella SHA-256 en la salida. Explica por que CN por si solo no basta para validar el nombre. Comparte solo esos datos publicos, nunca la clave ni contrasenas.

## Arranque

En la misma terminal configura las cuatro variables de la aplicacion conforme a scripts/README.md. DB_URL debe ser exactamente jdbc:postgresql://localhost:5432/reminishub_test. Desde la raiz:

```powershell
.\scripts\iniciar-https.ps1
```

El comando exige las variables, rechaza sustituciones directas de datasource/Flyway y muestra la base solicitada con contrasena oculta. Revisa que no existan otras configuraciones externas/JVM que sustituyan la conexion. El arranque puede aplicar migraciones pendientes. No utilizar reminishub_dev.

## Confianza y comprobacion

El certificado es autofirmado. El navegador puede rechazarlo hasta que confies explicitamente en el certificado publico. Una excepcion del navegador o curl -k solo demuestran conectividad, no validacion de identidad.

Para confianza limitada al comando, usa un curl con soporte para --cacert:

```powershell
curl.exe --cacert "$env:LOCALAPPDATA\ReminisHub\tls\localhost.crt" -i 'https://localhost:8443/proyectos?lang=en'
curl.exe --cacert "$env:LOCALAPPDATA\ReminisHub\tls\localhost.crt" -i 'https://localhost:8443/v3/api-docs'
```

Para el navegador, puedes importar manualmente localhost.crt en el almacen de certificados de confianza del usuario actual, tras verificar su huella. Esto es una decision explicita del estudiante. No importar el PKCS12 como autoridad ni desactivar globalmente la validacion. Si la politica del equipo impide confiar en el certificado, informar BLOCKED.

Comprueba catalogo ES/EN y Swagger en .\scripts\verificar-https.ps1, sin errores de certificado tras configurar confianza y sin contenido mixto. Verifica que el proceso escucha HTTPS en 8443 y no ofrece un conector HTTP adicional. Otros procesos ajenos pueden seguir ocupando 8080. Al finalizar detén Spring con Ctrl+C y elimina RH_TLS_PASSWORD de la terminal (`Remove-Item Env:RH_TLS_PASSWORD`).

## Evidencia y defensa

Con Spring activo, abre otra terminal en la raiz y ejecuta `./scripts/verificar-https.ps1`. El comando usa solo el certificado publico, verifica TLS y contenido de catalogo ES/EN, Swagger y OpenAPI. Si esas comprobaciones pasan, exige que un nombre de servidor incorrecto sea rechazado con error de certificado. No modifica la confianza de Windows. Los resultados HTTP quedan en target/https con un identificador por ejecucion. El recorrido de teclado y la confianza del navegador siguen requiriendo comprobacion interactiva.

Referencia del verificador: [manual oficial de curl](https://curl.se/docs/manpage.html), opciones --cacert y --resolve, codigo de salida 60.

Estado inicial: configuracion y guia preparadas; arranque PostgreSQL/TLS, confianza del navegador y recorrido HTTPS NOT RUN hasta su ejecucion real. Las 14 pruebas previas no verifican HTTPS.

[Verificado el 16/11] PASS de sintaxis de iniciar-https.ps1 mediante Parser.ParseFile. PASS del rechazo sin DB_URL, antes de ejecutar Maven. PASS de git diff --check y exclusiones Git de archivos .p12/.key. Arranque real BLOCKED en la terminal de Codex por variables privadas ausentes. No se genero certificado ni se cambio la confianza del sistema desde Codex.

Actualizacion: verificador ejecutado contra el servidor iniciado por el estudiante, con Windows PowerShell 5.1 y certificado publico local. PASS en las cuatro rutas y rechazo del nombre incorrecto, salida 0. Evidencia target/https/e38c8117cee84b43927b377534ae1e94. Se corrigio el tratamiento de stderr con --stderr de curl para permitir evaluar LASTEXITCODE=60. La verificacion automatica HTTPS queda completada; confianza/recorrido del navegador y defensa siguen pendientes.

Confirmacion posterior: el estudiante valida confianza en navegador. Keytool sobre certificado publico confirma SAN localhost/127.0.0.1, serverAuth, CA=false, RSA 3072 y vigencia 16/11/2026–16/12/2026. SHA-256: 8F:B1:01:08:78:D1:E1:37:D2:9F:DC:27:B4:C2:CD:0E:87:39:CB:74:4C:5C:E2:03:86:D5:22:4B:CF:5F:66:77. Las preguntas de defensa permanecen pendientes.

Preguntas de dificultad intermedia:
1. El certificado cifra correctamente, pero pertenece a otro nombre. ¿Debe el cliente aceptarlo y por que?
2. ¿Por que elegimos configurar confianza explicita en lugar de desactivar validacion TLS?
3. Una cookie es HttpOnly pero carece de Secure. ¿Que protege cada atributo y que falta probar?
4. ¿Como distingues un fallo de contrasena del PKCS12 de un certificado no confiable para el navegador?

Fuentes oficiales consultadas: [propiedades Boot](https://docs.spring.io/spring-boot/appendix/application-properties/) y [configuracion SSL](https://docs.spring.io/spring-boot/reference/features/ssl.html). Se utilizan propiedades server.ssl existentes, sin dependencias nuevas.
