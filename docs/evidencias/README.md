# Catalogo: revision del 13 de noviembre de 2026

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

Propuesta M6: quitar aria-label de la marca, conservando aria-hidden en el simbolo decorativo; cambiar los nombres de idioma a "ES — Español" y "EN — English". Repetir auditorias, verificar nombre accesible y teclado. El alcance es un fragmento compartido y pruebas pertinentes; sin cambios de seguridad o datos. Pendiente de aprobacion, no implementado.

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
