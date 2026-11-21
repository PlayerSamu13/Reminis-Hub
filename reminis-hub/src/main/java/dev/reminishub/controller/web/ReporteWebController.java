package dev.reminishub.controller.web;

import dev.reminishub.service.ReporteService;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Vistas administrativas y descarga CSV; la autorización también se comprueba en el servicio. */
@Controller
public class ReporteWebController {
    private final ReporteService reportes;
    public ReporteWebController(ReporteService reportes) { this.reportes = reportes; }

    /** Muestra filtros y agregados calculados en PostgreSQL, sin cargar entidades de proyecto. */
    @GetMapping("/admin/reporte")
    public String reporte(@RequestParam(required=false) Integer anio,
                          @RequestParam(required=false) String categoria, Model modelo) {
        var filas = reportes.consultar(anio,categoria);
        modelo.addAttribute("filas",filas);
        modelo.addAttribute("categorias", reportes.codigosDeCategoria());
        modelo.addAttribute("anio",anio);
        modelo.addAttribute("categoria",categoria == null ? "" : categoria);
        modelo.addAttribute("total",filas.stream().mapToLong(f -> f.total()).sum());
        modelo.addAttribute("ficticios",filas.stream().mapToLong(f -> f.ficticios()).sum());
        return "reporte";
    }

    /** Exporta exactamente los filtros de la vista; la ruta no modifica datos. */
    @GetMapping("/admin/reporte.csv")
    public ResponseEntity<byte[]> csv(@RequestParam(required=false) Integer anio,
                                     @RequestParam(required=false) String categoria, Locale idioma) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"reminishub-reporte.csv\"")
                .body(reportes.exportarParaExcel(anio,categoria,idioma));
    }

    /** Muestra eventos inmutables; no ofrece operaciones de edición o borrado del historial. */
    @GetMapping("/admin/bitacora")
    public String bitacora(@RequestParam(defaultValue="0") int pagina, Model modelo) {
        modelo.addAttribute("resultado",reportes.bitacora(pagina));
        return "bitacora";
    }
}
