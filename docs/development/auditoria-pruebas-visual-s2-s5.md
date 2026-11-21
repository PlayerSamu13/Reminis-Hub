# Auditoría de pruebas e interfaz S2–S5 — 20/11/2026

## Dictamen de las pruebas

[Verificado] Se analizaron los 23 XML de Surefire con sufijo `d4f2629178474a8e82490ade9865b2a1`: 116 pruebas, 0 fallos, 0 errores, 0 omisiones. La ejecución fue realizada por el estudiante; el agente contrastó sus artefactos locales. No se ejecutó una nueva suite PostgreSQL en la terminal del agente.

Los commits `4dbfed8` y `62e5bc0` cambian exclusivamente configuración/preparación de dos suites. No eliminan aserciones, no aceptan errores como éxito, no añaden exclusiones ni sustituyen PostgreSQL por mocks.

- **Auditoría:** conserva pool JPA de una conexión, transacción obligatoria, actor autenticado, lectura de la fila generada por el trigger, rollback de usuario/proyecto/bitácora, igualdad de pg_backend_pid antes/después y ausencia del actor local en la conexión reutilizada. Flyway usa una conexión separada únicamente para inicializar el esquema. Su configuración referencia variables de entorno sin persistir valores. Esto repara el bloqueo de arranque sin debilitar la propiedad comprobada.
- **Sesión:** cada caso representa un cliente independiente mediante una IP documental simulada. Conserva BCrypt, JWT real, 403 sin CSRF, cookie eliminada al salir, rechazo 401 del token revocado, segunda sesión todavía válida y rechazo de cuenta inactiva. El limitador no se desactiva ni se simula en esa suite.
- **Límite de registro:** IntentosRegistroTests verifica cinco intentos permitidos, sexto bloqueado, aislamiento por IP, expiración y saturación. RegistroWebTests comprueba 429 y uso de remoteAddr frente a encabezados aportados por el cliente; allí el limitador está simulado para probar el controlador. No se presenta esto como una prueba E2E del sexto registro real.

[Limitación] No se ejecutó análisis de mutaciones. Mantener aserciones relevantes y observar PASS no demuestra que toda regresión imaginable sea detectada. Auditoría inserta mediante EntityManager SQL nativo; PropuestasIntegrationTests complementa con servicios/JPA reales. Los casos concurrentes reales y accesibilidad completa no quedan certificados por este resultado.

## Revisión visual realizada

[Verificado: navegador integrado, HTTPS, sesión ADMIN] Inspección de propuestas, formulario, catálogo, detalle público, moderación y reporte. Vistas examinadas en anchos de 320, 390, 768 y 1440 px, con distintos recorridos ES/EN. No se recorrió cada combinación de ruta, ancho e idioma. Se restauró el tamaño del navegador al terminar.

- Bootstrap y CSS propio cargan. Acciones principales azules, secundarias con borde; enlaces de retorno estilizados.
- No se observó desbordamiento horizontal de página en las mediciones de formulario/lista/reporte. La tabla del reporte tiene desplazamiento interno y tabindex=0.
- Los controles del formulario inspeccionados tienen label asociado. Se añadió y retiró un coautor vacío sin enviar el formulario: índices y nombres accesibles correctos.
- Tab desde añadir coautor lleva a la aceptación y muestra contorno visible. Es una comprobación puntual, no una auditoría completa de teclado.
- La presentación de datos ficticios se distingue en el detalle público. No se publicaron, retiraron ni modificaron proyectos durante esta revisión.

## Hallazgos y propuesta para aprobación

| ID | Prioridad | Evidencia | Cambio propuesto |
|---|---|---|---|
| VIS-01 | Media | Cabecera ADMIN de ~282 px a 320 px de ancho, con navegación repartida en varias filas. | Compactar navegación móvil con un control de expansión accesible; conservar acceso a todas las rutas y foco visible. |
| VIS-02 | Media | Campos de propuestas/reporte con borde #dee2e6 sobre blanco y fondo muy claro; delimitación visual tenue. | Reforzar bordes de controles, unificar alturas y medir contraste antes/después. No afirmar incumplimiento WCAG solo por observación visual. |
| VIS-03 | Media | Reporte pide escribir WEB/MOVIL/etc. en un campo de texto. | Selector de categorías con etiquetas ES/EN y códigos internos conservados; consulta y exportación sin cambios de contrato. |
| VIS-04 | Baja | Menú principal carece de aria-current y señal visual de ruta activa. | Marcar la sección activa de manera coherente, incluida navegación por teclado. |
| VIS-05 | Baja | Catálogo muestra Crear cuenta con sesión ADMIN iniciada. | Mostrar Presentar proyecto a usuarios autenticados y Crear cuenta a visitantes. |
| VIS-06 | Baja | /admin/propuestas tiene h1 de moderación pero title Mis propuestas. | Título de pestaña específico para moderación en ES/EN. |
| VIS-07 | Baja | Formulario ofrece 36 tecnologías y requiere bastante desplazamiento. | Mejorar agrupación y espaciado responsive sin añadir una dependencia ni ocultar opciones obligatorias. |

[Valoración] La interfaz es funcional y mantiene una jerarquía consistente, pero los módulos nuevos aún necesitan homogeneidad de controles y navegación para alcanzar el estilo minimalista solicitado. El espacio vacío de listas sin datos es un estado normal; no justifica contenido artificial.

No se ejecutaron axe ni Lighthouse en esta revisión, ni se certifica WCAG 2.2 AA. Tampoco se observaron aquí los controles de decisión sobre una propuesta pendiente real, pues la lista de moderación estaba vacía. Las mejoras anteriores se proponen; no se implementaron durante esta auditoría.

## Siguiente incremento propuesto

Aplicar VIS-01–06 como corrección visual acotada; VIS-07 opcional. Archivos: layout.html, reminishub.css, catalogo.html, propuestas-lista.html, reporte.html, ReporteWebController y bundles ES/EN según necesidad. Riesgos: ocultación accidental del menú, estado activo incorrecto, divergencia entre filtros CSV/HTML. Verificar renderizado ES/EN, selector/filtros, teclado, 320/390/768/1440 px y ausencia de desbordamientos. No tocar autenticación, DTO de escritura ni migraciones para un ajuste visual.
