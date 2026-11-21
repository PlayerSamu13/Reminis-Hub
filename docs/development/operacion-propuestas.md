# Operación y comprobación de propuestas

## Alcance

S2 incorpora V7 y actor transaccional; S3 presenta propuestas; S4 permite consulta privada, edición del propietario, moderación ADMIN y retiro lógico; S5 consulta bitácora y reportes JDBC/CSV. El reenvío de rechazados sigue fuera del incremento. Este documento describe el uso previsto; los resultados ejecutados se registran en trazabilidad, no se deducen de la existencia de rutas.

## Base y migración

Usar únicamente `jdbc:postgresql://localhost:5432/reminishub_test` para las pruebas. La V7 definitiva está en recursos Flyway. El siguiente arranque/prueba Spring aplica V7 automáticamente después de validar V1–V6. No instalarla permanentemente con psql ni usar repair. Antes de arrancar, comprobar variables privadas en la terminal sin publicarlas y mantener detenida cualquier instancia que pueda reiniciar por DevTools.

El verificador SQL `docs/development/sql/verificar_s2.sql` detecta si V7 ya está aplicada: en ese caso prueba los triggers existentes; si no, los crea dentro de su transacción y revierte. Sus fixtures son sintéticos, no consentimientos reales. Los avances de secuencia no se revierten.

`web_user` es propietario de las tablas en el entorno actual, según salida del estudiante. Los triggers frenan operaciones ordinarias inválidas, pero el propietario puede alterarlos/desactivarlos y puede establecer contextos SQL arbitrarios. No exponer este rol a usuarios finales; separar propietario de migraciones y rol de ejecución es un pendiente de despliegue que requiere privilegios administrativos. No se afirma resistencia frente a un administrador de BD comprometido.

## Demostración web

1. Iniciar sesión local o con Google. Abrir **Mis propuestas → Nueva propuesta**.
2. Completar metadatos, categoría, tecnologías y autorización. El servidor asigna propietario, autor propio, estado pendiente y versión de términos. Coautores externos requieren referencia privada de autorización; no se suben archivos.
3. Consultar el detalle privado. Editar solo como propietario y mientras esté pendiente/rechazado. La versión enviada detecta una vista obsoleta; ante conflicto, recargar y revisar los datos antes de intentar otra vez.
4. Iniciar sesión como ADMIN. Revisar pendientes, metadatos y referencias antes de aprobar; rechazar exige motivo. La aplicación registra declaraciones, no verifica por sí sola la existencia material del consentimiento.
5. Un proyecto publicado aparece en catálogo. Retirar cambia a RETIRADO y conserva bitácora; no ejecutar esta acción sobre ejemplos que se quieran conservar visibles.
6. Revisar bitácora, reporte filtrado y CSV. Los ejemplos ficticios se cuentan por separado; los conteos no son resultados de usabilidad.

Para aprovisionar un administrador de demostración, un operador autorizado puede ejecutar `docs/development/sql/aprovisionar_admin.sql` con psql y `-v correo=...`, solo contra test y una cuenta activa existente. Es una acción manual de elevación de rol: no se ejecuta automáticamente ni desde un formulario. La cuenta debe iniciar sesión de nuevo. No enviar la contraseña por chat ni guardarla en el comando.

## Swagger sin desactivar CSRF

1. Iniciar sesión desde `/login` en el mismo navegador/origen HTTPS.
2. Abrir `/swagger-ui/index.html` y ejecutar `GET /api/v1/csrf`.
3. Copiar **token** en **Authorize → csrf**. No copiar RH_TOKEN ni contraseña. La cookie HttpOnly de autenticación la envía el navegador.
4. Probar `/api/v1/propuestas`: POST crea, GET lista/detalla, PUT actualiza y DELETE con versión retira lógicamente. `/api/v1/propuestas/{id}/decision` requiere ADMIN.
5. La ausencia de autenticación devuelve 401; permiso/CSRF insuficiente 403; datos inválidos 400; propuesta inexistente/ajena 404; conflicto de versión/estado/unicidad 409. Para solicitudes inseguras sin CSRF puede observarse 403 antes de comprobar la identidad.
6. Tras logout/login, obtener nuevo CSRF. Si falta cookie válida, Authorize/csrf no concede autenticación.

Las respuestas públicas no reutilizan el detalle privado ni sus referencias. API de reporte y bitácora bajo `/api/v1/admin/` requiere ADMIN.

## Pruebas y límites

Ejecutar desde raíz `./scripts/verificar.ps1 -Offline` en PowerShell con las variables privadas configuradas. El script restringe URL test y exige informes nuevos identificados por sufijo. No confundir las pruebas simuladas del verificador con las de PostgreSQL.

Las pruebas transaccionales revierten propuestas/bitácora y no las borran físicamente al terminar; de ese modo no evaden los triggers. Las pruebas de actor comprueban rollback y reutilización de una conexión JPA. La cobertura MockMvc verifica renderizado/contratos, no sustituye el recorrido manual de navegador, teclado, axe y Google real.

CSV: comillas duplicadas, campos delimitados y saltos CRLF, con prefijo defensivo ante fórmulas/controles iniciales. Importar como texto en hojas de cálculo. No existe garantía universal para todos los consumidores de CSV.

## Defensa técnica

- **¿Por qué set_config local?** El actor dura la transacción; evita que otra petición herede una identidad de una conexión reutilizada. El EntityManager comparte conexión con la escritura JPA.
- **¿Por qué servicio y trigger?** El servicio verifica permisos y ofrece errores comprensibles; el trigger protege integridad al escribir. Ninguno sustituye la separación de roles de BD.
- **¿Por qué no borrar?** RETIRADO conserva trazabilidad y es final. Un DELETE SQL rompería la historia que la aplicación debe mantener.
- **¿Por qué @Version?** Una edición obsoleta debe producir conflicto, no sobrescribir una decisión o edición más reciente.
- **¿Por qué JDBC para reporte?** Permite agregaciones SQL directas contrastables sin cargar grafos de entidades. JPA sigue coordinando el CRUD.
- **¿Por qué no basta con ocultar enlaces ADMIN?** Cualquier cliente puede fabricar peticiones; los filtros y servicios vuelven a comprobar identidad, rol y propiedad.
