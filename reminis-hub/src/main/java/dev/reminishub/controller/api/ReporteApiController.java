package dev.reminishub.controller.api;

import dev.reminishub.dto.FilaReporte;
import dev.reminishub.dto.PaginaBitacora;
import dev.reminishub.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** API administrativa de lectura; exige cookie JWT vigente y rol ADMIN en servidor. */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name="Administración: reporte y bitácora")
@ApiResponses({@ApiResponse(responseCode="401",description="Sin autenticación válida"),
        @ApiResponse(responseCode="403",description="Requiere ADMIN activo"),
        @ApiResponse(responseCode="400",description="Filtro o página inválidos")})
public class ReporteApiController {
    private final ReporteService reportes;
    public ReporteApiController(ReporteService reportes) { this.reportes = reportes; }

    /** @return agregados del catálogo completo, incluidos estados privados y datos ficticios */
    @GetMapping("/reporte")
    @Operation(summary="Cuenta proyectos por año, categoría y estado",description="ficticios es un subconjunto de total; filtros opcionales combinables.")
    public List<FilaReporte> reporte(@RequestParam(required=false) Integer anio,
                                     @RequestParam(required=false) String categoria) {
        return reportes.consultar(anio,categoria);
    }

    /** @return descarga UTF-8 con columnas localizadas y valores de dominio estables */
    @GetMapping(value="/reporte.csv",produces="text/csv")
    @Operation(summary="Exporta el reporte como CSV",description="Importar como texto en hojas de cálculo; los valores se escapan y las fórmulas se neutralizan preventivamente.")
    public ResponseEntity<byte[]> csv(@RequestParam(required=false) Integer anio,
                                      @RequestParam(required=false) String categoria, Locale idioma) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"reminishub-reporte.csv\"")
                .body(reportes.exportar(anio,categoria,idioma));
    }

    /** @return página de veinte eventos con fecha UTC y actor interno, sin datos de consentimiento */
    @GetMapping("/bitacora")
    @Operation(summary="Consulta la bitácora de proyectos",description="Orden descendente por fecha e ID. Solo hechos registrados desde la instalación de los triggers; no reconstruye historia previa.")
    public PaginaBitacora bitacora(@RequestParam(defaultValue="0") int pagina) {
        return reportes.bitacora(pagina);
    }
}
