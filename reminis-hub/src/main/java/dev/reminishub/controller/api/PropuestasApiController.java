package dev.reminishub.controller.api;

import dev.reminishub.dto.*;
import dev.reminishub.service.PropuestasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

/** CRUD privado sobre cookie de sesión; toda escritura conserva CSRF y autorización del servicio. */
@RestController
@RequestMapping("/api/v1/propuestas")
@Tag(name="Propuestas",description="Inicia sesión en el navegador. Obtén /api/v1/csrf y usa Authorize para enviar X-CSRF-TOKEN en escrituras.")
@ApiResponses({@ApiResponse(responseCode="401",description="Sin sesión válida"),
        @ApiResponse(responseCode="403",description="Permiso insuficiente o CSRF inválido"),
        @ApiResponse(responseCode="400",description="Datos inválidos"),
        @ApiResponse(responseCode="409",description="Versión, estado o repositorio en conflicto")})
public class PropuestasApiController {
    private final PropuestasService propuestas;
    public PropuestasApiController(PropuestasService propuestas){this.propuestas=propuestas;}

    @GetMapping
    @Operation(summary="Lista diez propuestas propias, o pendientes para ADMIN")
    public List<PropuestaResumen> listar(@RequestParam(defaultValue="0") int pagina,
            @RequestParam(defaultValue="false") boolean moderacion) {
        return propuestas.listar(pagina,moderacion);
    }
    @GetMapping("/{id}")
    @Operation(summary="Consulta una propuesta propia o accesible al administrador")
    @ApiResponse(responseCode="404",description="Inexistente o ajena al colaborador")
    public PropuestaDetalle detalle(@PathVariable long id){return propuestas.detalle(id);}

    @PostMapping
    @SecurityRequirement(name="csrf")
    @Operation(summary="Presenta una propuesta pendiente de revisión")
    @ApiResponse(responseCode="201",description="Propuesta creada; Location señala su detalle privado")
    public ResponseEntity<Void> crear(@Valid @RequestBody ProyectoForm formulario) {
        Long id=propuestas.crear(formulario);
        return ResponseEntity.created(URI.create("/api/v1/propuestas/"+id)).build();
    }
    @PutMapping("/{id}")
    @SecurityRequirement(name="csrf")
    @Operation(summary="Actualiza una propuesta pendiente/rechazada del propietario",description="Incluye version obtenida del detalle; no reenvía propuestas rechazadas.")
    @ApiResponse(responseCode="204",description="Actualizada")
    public ResponseEntity<Void> actualizar(@PathVariable long id,@Valid @RequestBody ProyectoForm formulario) {
        propuestas.actualizar(id,formulario); return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/{id}")
    @SecurityRequirement(name="csrf")
    @Operation(summary="Retira lógicamente una propuesta propia o como ADMIN",description="Retirado es final; no borra datos ni bitácora.")
    @ApiResponse(responseCode="204",description="Retirada")
    public ResponseEntity<Void> retirar(@PathVariable long id,@RequestParam long version) {
        propuestas.retirar(id,version); return ResponseEntity.noContent().build();
    }
    /** Decisión administrativa con versión del proyecto mostrado. */
    public record Decision(@NotNull @PositiveOrZero Long version,@NotNull Boolean aprobar,@Size(max=500) String motivo){}
    @PostMapping("/{id}/decision")
    @SecurityRequirement(name="csrf")
    @Operation(summary="ADMIN aprueba o rechaza una propuesta pendiente")
    @ApiResponse(responseCode="204",description="Decisión registrada y auditada")
    public ResponseEntity<Void> decidir(@PathVariable long id,@Valid @RequestBody Decision decision) {
        propuestas.decidir(id,decision.version(),decision.aprobar(),decision.motivo());
        return ResponseEntity.noContent().build();
    }
}
