> Nota de versión. Esta ficha describe la implementación inicial del 17/11. Para la defensa actual consulta [la guía de sustentación](guia-sustentacion.md), que incluye revocación por sesión y límite temporal de intentos.

# Acceso local (login, JWT y logout)

Implementado el 17/11/2026 en la rama `codex/registro-cierre`; sin migraciones ni dependencias nuevas. Pruebas automatizadas PASS; PostgreSQL real, navegador y aceptacion pendientes. Limitacion declarada: sin revocacion de tokens.

Peticion: `GET /login` pasa por `CsrfFilter`, que guarda el token en la sesion y lo imprime en el formulario; Thymeleaf resuelve idioma y bundles. El `POST` llega con `_csrf`: si falta, se responde 403 antes de tocar el JWT. Con el token, el controlador delega en `AuthenticationManager`, que compara la clave con BCrypt mediante `DaoAuthenticationProvider`. Si coincide, `TokenService` emite un JWT HS256 (emisor, asunto, nombre, rol, 60 min) y el controlador responde 302 con la cookie `RH_TOKEN`. En la siguiente peticion, `JwtCookieAuthenticationFilter` lee la cookie, verifica firma, emisor y expiracion, y construye la autoridad `ROLE_*`; `AuthorizationFilter` decide el acceso y `SesionAdvice` pinta el header. `POST /logout` con CSRF devuelve la misma cookie con `Max-Age=0`.

Decisiones y alternativas descartadas:
1. JWT en cookie `HttpOnly`/`Secure`/`SameSite=Lax` en lugar de `localStorage`. Se descarto `localStorage` porque cualquier XSS leeria el token; la cookie no es accesible desde JavaScript.
2. Filtro propio despues de `CsrfFilter` en lugar de `oauth2ResourceServer`. Se descarto este ultimo porque exime de CSRF toda peticion con bearer token: con la credencial en cookie, dejaba todos los POST autenticados sin proteccion. La traza de Spring lo confirmo antes de cambiar el diseno.
3. `STATELESS`: la sesion solo guarda el token CSRF; la identidad viaja en cada peticion. Se descarto la sesion de seguridad porque el contrato pide JWT propio.
4. Logout propio en lugar del filtro estandar, que interceptaba `POST /logout` y redirigia a `/?logout` sin conocer nuestra cookie.
5. Un unico 401 para correo inexistente, clave incorrecta y cuenta inactiva: no se revela que cuentas existen. El mensaje es localizado y no repuebla la contrasena.
6. Controlador delgado: el controlador traduce HTTP y el servicio emite el token; el repositorio solo busca la cuenta por correo normalizado.

Evidencia: `mvnw.cmd -o -Dtest=TokenServiceTests,AutenticacionWebTests,RegistroWebTests,RegistroServiceTests test` PASS, 36 pruebas, cero fallos. Cubren token con otra clave, token expirado, rol inventado en el claim, atributos de la cookie, 401 de API sin token, token alterado, logout sin CSRF (403 y la cookie sigue sirviendo), flujo real de CSRF tomando el valor impreso en el HTML y cuenta inactiva. Bundles 100/100. `git diff --check` PASS. `scripts/probar-verificador.ps1` 8/8. NOT RUN: `SesionE2ETests`, integraciones PostgreSQL, navegador y Lighthouse de `/login`, por falta de variables de base y de servidor en esta sesion.

Preguntas probables:
1. Por que el token no va en `localStorage`. Cualquier script inyectado puede leerlo; una cookie `HttpOnly` no se expone a JavaScript.
2. Por que no basta con activar `oauth2ResourceServer`. Ese filtro asume que el bearer viaja en cabecera y omite CSRF; con cookie, un sitio externo podria enviar POST con la sesion del usuario.
3. Por que el logout no invalida el token de inmediato. Sin estado en el servidor no hay lista de tokens vigentes; se mitiga con 60 minutos de vigencia y queda medido para decidir una revocacion.
4. Que impide fabricar un token con rol ADMIN. La firma HS256 con un secreto que solo conoce el servidor y la validacion del rol contra el enum del dominio.

Aporta criterios 2 (pruebas), 4 (hash, JWT, cookie, CSRF, autorizacion) y 5 (i18n ES/EN y formulario accesible). No acredita Google, roles en uso, CRUD, auditoria ni reporte. Limites: sin commit, sin prueba en navegador y sin PostgreSQL real en esta sesion. Tampoco hay limite de intentos de acceso ni revocacion de tokens.
