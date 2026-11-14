# Catalogo: evidencias de revision

## Revision del 14 de noviembre de 2026 — M6

[Verificado: fragments/layout.html y revision-catalogo.json] M6 aprobada por el estudiante: se elimina aria-label de la marca y los idiomas se nombran «ES — Español» y «EN — English». Tambien aprobo agregar role="group" al contenedor con nombre Idioma/Language. Se conserva el simbolo decorativo oculto a tecnologias de asistencia. Sin cambios de datos, seguridad o dependencias de la aplicacion.

| Comprobacion | Estado | Evidencia |
|---|---|---|
| Suite Java/PostgreSQL tras M6 inicial | PASS | 10 pruebas, 0 fallos/errores/omisiones; scripts/verificar.ps1 -Offline; sufijo 47fb8b52327c499991743a31735bdff5. El role posterior se verifica en navegador. |
| Axe independiente | PASS | axe-core 4.14.0 en Edge mediante Selenium: cuatro vistas, cero infracciones; regla experimental label-content-name-mismatch habilitada explicitamente. JSON axe-*.json. |
| Nombres accesibles finales | PASS | WebDriver comprueba marca, ES/EN y grupo en los dos idiomas; revision-catalogo.json conserva nombres calculados. |
| Lighthouse final | PASS | 13.6.0, catalogo ES y detalle EN: accesibilidad 100, label-content-name-mismatch=1 y ninguna auditoria binaria fallida. JSON del 14/11 15:57 UTC. |
| Sin JavaScript | PASS | Perfil temporal con scripts de pagina bloqueados: jQuery ausente antes y despues; paginas 6+2, detalle/regreso, texto y tres filtros por GET, cambio a EN conservando filtros y estado vacio. revision-catalogo.json y sin-javascript.png. |
| Revision de nodos incomplete | PASS | Selectores: contraste calculado 6,22:1. Restantes nodos son simbolos decorativos con informacion textual equivalente; inspeccion de HTML/CSS y captura. Alcance limitado a los nodos reportados. |

Los selectores usan texto rgb(83,98,118) sobre blanco: contraste calculado 6,22:1. El SVG es la flecha de Bootstrap a la derecha, no un fondo bajo el texto; los estilos calculados quedan en revision-catalogo.json. Se inspeccionaron los restantes nodos incomplete de catalogo y detalle: marca r., simbolo del titulo Filtros, lupa, marcas de Publicado y flechas de paginacion/regreso. Son decorativos, aria-hidden=true y acompanados de texto o etiqueta que expresa su funcion; no se depende de esos simbolos para comprender la accion/estado. No se eliminan advertencias del informe. No se certifican todos los estados visuales ni WCAG global. Las pruebas de teclado y responsive del 13/11 siguen como evidencia historica; la repeticion sin JS usa Enter para enlaces y envio, no acredita por si sola un recorrido completo con Tab o lector de pantalla.

Reproduccion del auditor local (Node, Edge y herramientas QA ya instaladas; no pertenecen al pom.xml):

```powershell
$env:RH_QA_MODULES = 'C:\ruta\a\node_modules' # contiene selenium-webdriver y axe-core 4.14.0
node .\scripts\auditar-catalogo.cjs
```

Servidor local esperado: http://127.0.0.1:8081, con los ocho ejemplos ficticios de esta base. RH_QA_URL permite otro servidor exclusivamente local. Selenium Manager resuelve el WebDriver compatible con Edge; puede requerir red la primera vez. El script cierra ambos navegadores y devuelve 1 si hay infracciones o falla el recorrido. `--sin-js` ejecuta solo ese recorrido y genera un resumen sin resultados axe; la ejecucion registrada aqui es completa. No se descargaron navegadores adicionales.

Fuentes: [Selenium Edge](https://www.selenium.dev/documentation/webdriver/browsers/edge/), [Edge WebDriver](https://learn.microsoft.com/en-us/microsoft-edge/webdriver/), [API axe-core](https://github.com/dequelabs/axe-core/blob/develop/doc/API.md).

Lighthouse se repitio con el CLI 13.6.0 ya instalado, `--chrome-flags="--headless=new" --port=9222`. El primer intento con puerto dinamico fallo antes de auditar; el reintento con puerto fijo completo las dos vistas. Estos resultados solo corresponden a accesibilidad automatica.

## Revision historica del 13 de noviembre de 2026

Codigo de aplicacion revisado: `f193465`. Solo datos ficticios; servidor temporal en 127.0.0.1:8081, detenido al finalizar. Las capturas no demuestran por si solas conformidad WCAG.

| Comprobacion | Estado | Evidencia |
|---|---|---|
| Paginacion | PASS | Siguiente muestra pagina 2 de 2 con Encuentro y Observatorio; foco en resultados. |
| Detalle | PASS | Tab y Enter desde resultados abre Encuentro; muestra autor ficticio, tecnologias, periodo y enlace de codigo. La incidencia anterior de navegacion no se reprodujo con el servidor activo. |
| Filtros combinados | PASS | WEB + Java + 2026 devuelve Aula Abierta. |
| Busqueda viva y vacio | PASS | Escribir SinCoincidenciaQA sin enviar formulario muestra cero resultados; enlace para restablecer catalogo. |
| ES/EN | PASS | Cambio a EN conserva filtros y texto buscado; vacio traducido. Detalles Aula/Encuentro/Campus inspeccionados durante la revision; el contenido de proyectos permanece en su idioma original. |
| Teclado | PASS | Salto al contenido y regreso al catalogo; recorrido de 14 controles del catalogo, orden coherente y outline solid; apertura de detalle con Enter. No es una prueba con lector de pantalla. |
| Responsive | PASS | Ficha a 390 px: scrollWidth 375; catalogo a 1366 px: scrollWidth 1351. Capturas adjuntas. No equivale a probar todos los dispositivos. |
| Lighthouse | FAIL | Lighthouse 13.6.0 finalizo catalogo ES y detalle EN. Categoria puntuada accessibility=1.0 en ambos, pero auditoria label-content-name-mismatch fallo para marca e idiomas. No confundir puntuacion con ausencia de hallazgos. |
| Axe independiente | BLOCKED | @axe-core/cli anuncia axe-core 4.14.0, pero falla antes de evaluar: spawn chromedriver.exe ENOENT. No existe resultado de axe independiente. |
| JavaScript desactivado | NOT RUN | La API del navegador integrado no ofrece desactivacion de JavaScript. El formulario GET y renderizado servidor estan inspeccionados, pero no sustituyen esta prueba interactiva. |

## Hallazgo para aprobar antes de corregir

`fragments/layout.html` define aria-label="ReminisHub" sobre una marca con lema visible; los enlaces ES/EN se llaman Espanol/English. Lighthouse identifica que el texto visible no esta contenido en el nombre accesible.

Propuesta M6 en esa fecha: quitar aria-label de la marca, conservando aria-hidden en el simbolo decorativo; cambiar los nombres de idioma a "ES — Español" y "EN — English". Aprobada e implementada el 14/11; ver revision superior. Los JSON actuales sustituyen las capturas de auditoria anteriores; los originales permanecen en el historial Git.

## Reproduccion de Lighthouse

Con la aplicacion activa, desde la raiz en PowerShell (Edge instalado):

```powershell
$env:CHROME_PATH = 'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'
npx.cmd --yes lighthouse@13.6.0 http://127.0.0.1:8081/proyectos --only-categories=accessibility --chrome-flags="--headless" --output=json --output-path=docs/evidencias/lighthouse-catalogo-es.json --quiet
npx.cmd --yes lighthouse@13.6.0 'http://127.0.0.1:8081/proyectos/8?lang=en' --only-categories=accessibility --chrome-flags="--headless" --output=json --output-path=docs/evidencias/lighthouse-detalle-en.json --quiet
```

El ID 8 corresponde a Campus Verde en esta base; usar un proyecto publicado existente en otra instalacion. Los JSON conservan version, URL, fecha y hallazgos. Se audito accesibilidad, no rendimiento ni SEO.

Axe se intento con `npm.cmd exec --yes --package=@axe-core/cli -- axe` y `--chrome-path` apuntando a Edge, `--save`, `--dir docs/evidencias`, `--exit`. Antes de repetir hay que disponer de navegador y WebDriver compatibles. No se descargo un navegador adicional ni se desactivo su sandbox. Lighthouse incluye comprobaciones basadas en axe, pero eso no se registra como una segunda ejecucion independiente.

Para la prueba manual pendiente sin JavaScript: en un navegador con esa opcion, desactivarlo solo para localhost, recargar catalogo, combinar filtros y Buscar, recorrer Siguiente/Anterior, abrir detalle y comprobar ambos idiomas; restaurarlo despues. Registrar resultado real, no asumir PASS.

Fuentes oficiales consultadas: [Lighthouse](https://github.com/GoogleChrome/lighthouse/blob/main/readme.md) y [axe CLI](https://github.com/dequelabs/axe-core-npm/blob/develop/packages/cli/README.md).
