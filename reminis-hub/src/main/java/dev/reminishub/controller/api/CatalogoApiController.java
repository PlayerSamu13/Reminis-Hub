package dev.reminishub.controller.api;

import dev.reminishub.dto.PaginaProyectos;
import dev.reminishub.dto.ProyectoDetalle;
import dev.reminishub.service.CatalogoService;
import org.springframework.web.bind.annotation.*;

/** Lectura pública del catálogo. Las operaciones de escritura no se exponen en esta etapa. */
@RestController
@RequestMapping("/api/v1/proyectos")
public class CatalogoApiController {
    private final CatalogoService catalogo;
    public CatalogoApiController(CatalogoService catalogo) { this.catalogo = catalogo; }

    @GetMapping
    public PaginaProyectos buscar(@RequestParam(defaultValue = "") String q,
                                  @RequestParam(defaultValue = "") String categoria,
                                  @RequestParam(required = false) Integer anio,
                                  @RequestParam(required = false) Long tecnologia,
                                  @RequestParam(defaultValue = "0") int pagina) {
        return catalogo.buscar(q, categoria, anio, tecnologia, pagina);
    }

    @GetMapping("/{id}")
    public ProyectoDetalle detalle(@PathVariable long id) { return catalogo.detalle(id); }
}
