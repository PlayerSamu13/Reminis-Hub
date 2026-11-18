# Registro local — cierre

Etapa aprobada por el estudiante el 17/11/2026, con la comprobacion de comprension aplazada expresamente. Rama `codex/registro-cierre`; sin migraciones ni dependencias nuevas. Registro completo automatizado PASS; durabilidad en PostgreSQL NOT RUN en esta sesion.

Peticion: el navegador pide GET /registro sobre HTTPS; Spring Security permite la ruta, CSRF genera el token oculto y Thymeleaf resuelve idioma, plantilla y bundles. El POST llega con `_csrf`; el controlador valida `RegistroForm`, el servicio normaliza el correo a minusculas, comprueba duplicado, cifra con BCrypt coste 12 y guarda en una transaccion. Responde 302 a /registro; ese GET consume el flash y muestra el aviso de cuenta creada. Errores: 400 en la misma vista, 409 por correo ocupado, 403 sin token CSRF. La contrasena no se repuebla nunca en HTML.

Decisiones y alternativas descartadas:
1. `aria-describedby` condicional. Thymeleaf elimina los `th:errors` sin error; deja referencias colgantes. Se descarto dejar parrafos vacios porque anaden nodos sin contenido al arbol de accesibilidad.
2. `is-invalid` e `invalid-feedback` de Bootstrap en lugar de CSS propio. Bootstrap ya aporta borde, icono y contraste sin codigo nuevo; se descarto una regla `[aria-invalid="true"]` que duplicaba estilos.
3. Foco al primer campo invalido con `th:autofocus` condicional, en orden nombre, correo, clave. Se descarto JavaScript en linea porque rompe una CSP futura. El error de bytes tambien enfoca la clave.
4. `meta description` por vista, tomada del resumen real en el detalle. Se descarto un texto global repetido.
5. Prueba de extremo a extremo sin `@Transactional`: la peticion confirma y la comprobacion posterior abre otra transaccion. Con transaccion de prueba solo se probaria el INSERT.

Evidencia: `mvnw.cmd -o -Dtest=RegistroWebTests,RegistroServiceTests test` PASS, 17 pruebas (12 web, 5 servicio), cero fallos. Navegador real en https://localhost:8443: formulario limpio sin `autofocus` ni referencias colgantes; tras 400 los campos quedan con `is-invalid`, borde `rgb(220,53,69)` y descripciones existentes; el error de solo bytes enfoca la contrasena. Lighthouse accesibilidad 1, buenas practicas 1, SEO 1 en /proyectos y /registro. Bundles 87/87. `git diff --check` PASS. `RegistroE2ETests` NOT RUN: exige `CATALOGO_DB_TESTS=true` y PostgreSQL real.

Preguntas probables:
1. Por que el token CSRF si la contrasena ya viaja cifrada por TLS. TLS protege el trayecto; el CSRF impide que otro sitio aproveche la cookie de sesion del usuario para enviar el formulario sin su intencion.
2. Que demuestra `PasswordEncoder.matches` y que no. Demuestra que la contrasena presentada corresponde al hash guardado; no demuestra autorizacion, ni que la cuenta este activa, ni la fortaleza de la frase elegida.
3. Por que dos registros simultaneos con el mismo correo no duplican la fila. La consulta previa puede desactualizarse; la restriccion unica de la base decide y la excepcion se traduce en un error comprensible.

Aporta criterios 2 (pruebas), 4 (hash, CSRF, HTTPS) y 5 (i18n, accesibilidad). No acredita login, JWT, roles ni Google: siguen pendientes. Limites declarados: comprension aplazada, sin commit y sin comprobacion de durabilidad en esta sesion.
