# Estabilizacion del catalogo — 16/11/2026

El estudiante delego el cierre tras la revision y autorizo continuar con HTTPS. Defensa revisada, con precision sobre aserciones e independencia de pruebas.

Una peticion GET del catalogo pasa por el controlador, CatalogoService y repositorios. El servicio obtiene tambien las opciones ordenadas. La plantilla selecciona singular/plural y diferencia ausencia de coincidencias de pagina fuera del total. El enlace de recuperacion conserva filtros e idioma.

Se habilitaron GET de OpenAPI/Swagger conservando CSRF. La configuracion explicita impide incluir mensajes internos y trazas en respuestas de error. El README documenta variables y arranque.

[Verificado: Surefire 51e66be439874f20b3460077612bd9c0] 14 pruebas, cero fallos, errores u omisiones. [Declarado por el estudiante] Swagger funcional y recorrido Tab/Enter correcto. Su prueba validarEstadoVacioEn comprueba 200, contador cero y mensaje ingles. No prueba recuperacion de pagina: esas aserciones pertenecen a paginaFueraDeRangoPermiteVolverConFiltros.

Decisiones: consultas coordinadas desde servicio, mensajes distintos por estado y recuperacion con filtros. Alternativas descartadas: acceder a repositorios desde controlador o esconder la paginacion sin una recuperacion clara.

Preguntas de defensa:
1. ¿Por que servicio? Centraliza las operaciones y el limite transaccional, desacoplando la web de persistencia.
2. ¿Por que distinguir vacios? Puede haber coincidencias aunque el indice solicitado exceda el total.
3. ¿Una prueba vacia detecta un enlace perdido? Solo si sus aserciones comprueban ese enlace. La independencia entre pruebas es otra propiedad.

Aportes a rubrica: frontend, backend y traduccion/usabilidad. Seguridad propia, CRUD, triggers, reporte y estudio de usuarios siguen pendientes.
