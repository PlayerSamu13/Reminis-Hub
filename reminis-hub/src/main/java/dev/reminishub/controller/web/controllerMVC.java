package dev.reminishub.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import dev.reminishub.service.CatalogoService;
import dev.reminishub.repository.CategoriaRepository;
import dev.reminishub.repository.TecnologiaRepository;

@Controller
public class controllerMVC {
    private final CatalogoService catalogo;
    private final CategoriaRepository categorias;
    private final TecnologiaRepository tecnologias;

    public controllerMVC(CatalogoService catalogo, CategoriaRepository categorias, TecnologiaRepository tecnologias) {
        this.catalogo = catalogo;
        this.categorias = categorias;
        this.tecnologias = tecnologias;
    }

    /** Catálogo completo y fragmento AJAX comparten consulta y plantilla de resultados. */
    @GetMapping({"/", "/proyectos", "/catalogo/resultados"})
    public String inicio(@RequestParam(defaultValue = "") String q,
                         @RequestParam(defaultValue = "") String categoria,
                         @RequestParam(required = false) Integer anio,
                         @RequestParam(required = false) Long tecnologia,
                         @RequestParam(defaultValue = "0") int pagina,
                         jakarta.servlet.http.HttpServletRequest request, Model model) {
        model.addAttribute("resultado", catalogo.buscar(q, categoria, anio, tecnologia, pagina));
        model.addAttribute("q", q);
        model.addAttribute("categoria", categoria);
        model.addAttribute("anio", anio);
        model.addAttribute("tecnologia", tecnologia);
        if (request.getRequestURI().endsWith("/catalogo/resultados"))
            return "fragments/resultados :: resultados";
        model.addAttribute("categorias", categorias.findByActivaTrueOrderByOrdenAscCodigoAsc());
        model.addAttribute("tecnologias", tecnologias.findAllByOrderByNombreAsc());
        model.addAttribute("anios", catalogo.anios());
        return "catalogo";
    }

    @GetMapping("/proyectos/{id}")
    public String detalle(@PathVariable long id, Model model) {
        model.addAttribute("proyecto", catalogo.detalle(id));
        return "detalle";
    }
}
