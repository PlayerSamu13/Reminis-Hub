# Recursos locales

- Bootstrap CSS 5.3.8: https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css
- jQuery 3.7.1 completo (incluye AJAX): https://code.jquery.com/jquery-3.7.1.min.js

Se conservan las cabeceras originales y las licencias MIT adjuntas. No se requieren CDN ni fuentes remotas al usar el catálogo. No se necesita el JavaScript de Bootstrap porque esta etapa no utiliza sus componentes interactivos.

Adaptacion local: se retiro exclusivamente el comentario final `sourceMappingURL=bootstrap.min.css.map` del CSS de Bootstrap. El mapa no se distribuye en este proyecto; anunciarlo producia un 404 al depurar. No se alteraron las reglas CSS ni la licencia. Las herramientas del navegador siguen mostrando el CSS compilado, pero no su correspondencia con los fuentes Sass originales.
