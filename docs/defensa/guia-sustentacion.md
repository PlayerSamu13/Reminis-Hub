# ReminisHub: guía de estudio y sustentación

Actualizada el 18/11/2026. Describe el código actual, no funciones futuras. La preparación pasa a ser escrita por solicitud del estudiante: no exige respuestas al terminar cada etapa. Leer esta guía no demuestra dominio; al estudiar, explica y ejecuta los recorridos sin memorizar frases.

## 1. Qué se puede demostrar

ReminisHub permite consultar proyectos académicos publicados, filtrarlos y ver su detalle. Un visitante puede crear una cuenta de colaborador e iniciar sesión local. Las propuestas, moderación, Google y reportes todavía no están implementados. Tener sus entidades o dependencias no equivale a tener esos flujos.

El registro se cerró en la sesión anterior según trazabilidad. La autenticación sigue en revisión. Los informes Surefire `effa2ed51b7b40d8b1f4c36b5dc2a1ee` contienen 58 pruebas aprobadas, incluidas RegistroE2ETests y SesionE2ETests: son evidencia anterior a los ajustes del 18/11, no una ejecución de hoy.

## 2. Arquitectura que debes dibujar

```text
Navegador -> filtros de Spring Security -> controlador -> servicio -> repositorio -> PostgreSQL
                                             |
                                       DTO / modelo
                                             |
                                 Thymeleaf HTML o JSON
```

- **Controlador:** interpreta la petición y elige respuesta o vista. No contiene SQL.
- **Servicio:** coordina reglas de negocio y transacciones.
- **Repositorio:** consulta o persiste entidades mediante JPA.
- **Entidad:** representa persistencia; no es el contrato público de la API.
- **DTO:** datos seleccionados para una respuesta. Un `record` sirve para resultados inmutables; el formulario de registro es mutable porque Spring rellena campos y sus setters normalizan entradas.
- **Transacción:** confirma cambios juntos o los revierte ante un fallo. `flush` envía SQL, pero no significa `commit`.

Esta separación ya aporta suficiente abstracción. No se necesitan interfaces para cada servicio ni controladores genéricos: añadirían saltos sin resolver un problema presente. `SecurityConfig`, `JwtConfig` y el filtro tienen responsabilidades técnicas distintas; conviene estudiarlos en ese orden.

## 3. Catálogo: recorrido de una petición

Abre `/proyectos?q=Aula&lang=en`. `ControllerMVC` solicita a `CatalogoService` los resultados; `ProyectoRepository` combina filtros y exige PUBLICADO. El servicio entrega DTO y Thymeleaf renderiza el HTML. `WebConfig` interpreta `lang` en GET y conserva el idioma en cookie; no es un parámetro que cada controlador deba procesar.

`catalogo.js` mejora el formulario: espera 300 ms, cancela solicitudes previas y sustituye el fragmento de resultados. La cancelación más un contador de generación evitan que una respuesta vieja reemplace la búsqueda nueva. Sin JavaScript siguen funcionando los GET normales.

**Defensa:** ocultar tarjetas no protege datos. La consulta del servidor debe excluir estados privados; el detalle también debe rechazarlos. DTO evita enviar por accidente correos, hashes o motivos administrativos.

## 4. Registro: del formulario a una fila

1. GET `/registro` genera un formulario con token CSRF.
2. POST aplica binding a `RegistroForm`, limitado a nombre, email y contraseña.
3. Jakarta Validation comprueba formato y longitud. Nombre y correo se normalizan; la contraseña no se recorta.
4. `RegistroService` vuelve a validar, consulta duplicados, calcula BCrypt y asigna COLABORADOR/activo en el servidor.
5. `saveAndFlush` provoca el INSERT dentro de la transacción. Se traduce únicamente la unicidad de correo (`23505`, `uq_usuario_email`). Otras restricciones no se disfrazan de correo duplicado.
6. Tras confirmar, el controlador redirige. Un mensaje flash se consume en el GET siguiente; recargar ese GET no repite el POST.

**Por qué existe la restricción SQL:** dos peticiones pueden consultar el mismo correo antes de que ninguna inserte. `existsByEmail` mejora el mensaje, pero solo UNIQUE impide las dos filas.

**Contraseña:** BCrypt almacena un hash con sal, no un cifrado reversible. Se verifica con `matches`. El límite del algoritmo se mide en bytes UTF-8; los caracteres Unicode pueden ocupar varios. La validación actual de longitud usa unidades UTF-16, no caracteres visuales: no afirmar lo contrario.

**Pruebas:** las transaccionales permiten revertir fixtures; RegistroE2ETests no envuelve toda la petición en una transacción de prueba, por lo que comprueba persistencia tras commit y confirmación de un solo uso. No confundir MockMvc con un navegador ni con TLS real.

## 5. Login, JWT y logout

POST `/login` pasa primero por CSRF. `LoginController` entrega las credenciales a `AuthenticationManager`; `DaoAuthenticationProvider` obtiene la cuenta mediante `UserDetailsService`, rechaza cuentas inactivas y compara BCrypt. `TokenService` emite el JWT firmado con HS256/Nimbus y una cookie RH_TOKEN.

En solicitudes posteriores, `JwtCookieAuthenticationFilter` lee esa cookie, el decoder valida firma, emisor y tiempo, y el conversor traduce el rol admitido a una autoridad de Spring. `SesionAdvice` prepara los datos de cabecera; `/api/v1/sesion` devuelve el DTO de identidad. No se valida un claim de audiencia: el comentario anterior que lo afirmaba se corrigió.

| Concepto | Explicación precisa |
|---|---|
| JWT firmado | Detecta cambios; su contenido puede leerse. No guarda la contraseña. |
| HttpOnly | Impide que JavaScript lea la cookie; no vuelve imposible un ataque XSS. |
| Secure | Envía la cookie por HTTPS. No activa ni configura HTTPS por sí solo. |
| SameSite=Lax | Reduce ciertos envíos entre sitios; no sustituye CSRF. |
| CSRF | Evita aceptar una operación insegura solo porque el navegador adjuntó cookies. |
| STATELESS | El contexto de autenticación no se conserva en sesión; puede existir sesión para CSRF y flash. |
| 401 / 403 | Sin autenticación válida / operación no permitida. CSRF inválido también puede producir 403. |

La API debe devolver 401 incluso si el cliente pide HTML; las vistas pueden dirigir al login. Una regresión del 18/11 reprodujo un 302 incorrecto y motivó priorizar la ruta `/api/**`.

Cada login emite un identificador de sesión único (jti). Al cerrar sesión, el servidor registra ese identificador en PostgreSQL y después ordena borrar la cookie. La siguiente petición con una copia encuentra la revocación y queda anónima. Otra sesión del mismo usuario tiene un identificador distinto. La migración V6 crea la tabla y su índice de limpieza. Cada logout elimina las entradas caducadas tras un margen de cinco minutos. Las cookies emitidas antes de introducir jti requieren iniciar sesión de nuevo.

El filtro también verifica que la cuenta siga activa y conserve el rol firmado. El login admite cinco comprobaciones por correo normalizado en una ventana de 60 segundos. La sexta recibe 429 con Retry-After. El contador vive en memoria, se reinicia con el proceso y se aplica a una instancia. En caso de saturación de las 4096 ventanas, los correos nuevos esperan hasta que expire la primera.

## 6. Interfaz y accesibilidad

Un enlace `<a>` navega aunque se dibuje como botón; `<button type="submit">` envía el formulario. Las clases Bootstrap son presentación, no autorización. El estilo usa espacios consistentes, bordes discretos, contraste y tipografía del sistema; no requiere otro framework.

Cada campo tiene label. `aria-describedby` solo referencia elementos presentes. Los errores se explican mediante texto, no solo rojo, y se dirige el foco a un único campo inválido. Los bundles ES/EN contienen los mensajes. `th:text` escapa el contenido del usuario.

Un 100 de Lighthouse o cero infracciones de axe no prueban conformidad total: faltan estados, recorridos manuales y módulos futuros. Una advertencia del navegador tampoco se corrige ocultándola; hay que identificar su recurso o su origen.

## 7. Base de datos y evidencia

- Flyway versiona el esquema; una migración aplicada no se edita. La siguiente se determina mirando los archivos, no suponiendo su número.
- Hibernate `validate` verifica correspondencia del modelo, no todas las reglas del negocio.
- Las pruebas usan exclusivamente `reminishub_test`; `reminishub_dev` se considera producción por decisión del estudiante.
- Los triggers de bitácora y reportes JDBC siguen pendientes. No afirmar que JPA ya los proporciona.
- PASS requiere una ejecución y un resultado identificado; NOT RUN no significa FAIL ni PASS. La suite actual exige 64 pruebas con revocación y ventana de intentos.

## 8. Guion de demostración (8–10 minutos)

1. Explica problema, público y alcance implementado (un minuto).
2. Busca, combina filtros, cambia idioma y abre detalle; explica qué valida el servidor.
3. Muestra registro correcto y un caso inválido con datos ficticios en base de pruebas.
4. Inicia sesión, muestra el cambio de cabecera y cierra sesión; no proyectes valores de cookies ni secretos.
5. Presenta una prueba de CSRF y otra de rol manipulado; explica la aserción, no solo el verde.
6. Muestra el recorrido por capas y una restricción SQL. Termina con pendientes y límites reales.

## 9. Preguntas para practicar con respuesta orientativa

1. **¿Por qué DTO y no entidad?** Controla exactamente los datos expuestos y desacopla persistencia del contrato.
2. **¿Por qué no basta validar en Java?** Otros clientes o peticiones concurrentes pueden alcanzar la base; constraints protegen invariantes.
3. **¿Por qué CSRF con JWT?** El riesgo depende de cómo viaja: la cookie se adjunta automáticamente.
4. **¿Qué falla si `flush` se confunde con commit?** Podrías anunciar una operación persistida que aún puede revertirse.
5. **¿Qué ocurre si se copia un JWT antes de logout?** Aunque la firma siga siendo válida, el filtro rechaza el identificador registrado al cerrar esa sesión.
6. **¿Un administrador desactivado pierde acceso inmediatamente?** En la siguiente petición autenticada el filtro consulta la cuenta y rechaza el acceso mientras siga inactiva.
7. **¿Qué prueban 58 pruebas aprobadas ayer?** Solo sus casos y esa versión; no el código modificado hoy ni los módulos ausentes.
8. **¿Por qué conservar un monolito?** Reduce infraestructura y permite seguir una petición completa sin repartir un problema pequeño entre servicios.

## 10. Cómo estudiar al finalizar

Primero reproduce cada recorrido. Después localiza sus archivos y explica una decisión y una alternativa. Por último, ejecuta un caso negativo y predice el resultado antes de verlo. Usa las fichas de etapa como referencia; registra las dudas que no puedas justificar con código o evidencia. No memorices que todo está terminado si quedan requisitos pendientes.
