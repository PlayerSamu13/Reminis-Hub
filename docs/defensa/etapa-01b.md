# E1B — Catalogo publico

Aceptada expresamente por el estudiante el 14/11/2026. Defensa respondida y revisada.

## Recorrido de una peticion

GET /proyectos recibe texto, filtros y pagina. El controlador delega la consulta en CatalogoService; el servicio limita los resultados a PUBLICADO, consulta el repositorio y devuelve DTO. Thymeleaf presenta el catalogo; jQuery solicita el fragmento de resultados durante la busqueda. Sin JavaScript, el formulario GET mantiene la consulta funcional. La API publica entrega DTO y el detalle comprueba visibilidad.

## Decisiones y alternativas

- Filtrar PUBLICADO en servidor: ocultar tarjetas en el navegador no impediria consultar datos privados.
- DTO: contrato explicito sin exponer entidades JPA completas; no reemplaza autorizacion.
- Thymeleaf y mejora progresiva con jQuery: permite navegar sin JavaScript, sin introducir una SPA.
- Recursos locales y mensajes ES/EN: demostracion sin depender de CDN y traduccion de interfaz; el contenido de proyectos conserva su idioma original.
- Nombre accesible de la marca derivado del contenido; ES/EN incluyen su texto visible y pertenecen a un grupo con nombre. No basta obtener puntuacion 100 en Lighthouse.

## Evidencia y limites

[Verificado] 10 pruebas Java/PostgreSQL sin fallos, errores u omisiones; sufijo Surefire 47fb8b52327c499991743a31735bdff5. Axe: cuatro vistas ES/EN sin infracciones automaticas; Lighthouse: catalogo ES y detalle EN con nombres corregidos. Recorrido sin JavaScript y revision de nodos incomplete documentados en docs/evidencias/README.md. La adicion final de role=group se verifico con WebDriver despues de la suite Java. No acredita autenticacion propia, CRUD privado, auditoria, reportes ni conformidad WCAG global.

## Tres preguntas de defensa

1. ¿Por que filtrar en servidor? Porque el cliente puede omitirse o manipularse; el servidor debe restringir los datos.
2. ¿Por que DTO y no entidades? Para definir el contrato publico y evitar exponer campos o relaciones internos.
3. ¿Diez pruebas y Lighthouse 100 prueban todo el proyecto? No: solo cubren casos y vistas evaluados; faltan otros modulos y validacion humana.

Rubrica: aporta a frontend, backend y datos mediante RF-01–RF-04; aporta parcialmente a i18n y accesibilidad. No declara completados los criterios globales.
