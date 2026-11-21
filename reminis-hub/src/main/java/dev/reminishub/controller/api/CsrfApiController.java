package dev.reminishub.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

/** Permite probar escrituras en Swagger sin desactivar CSRF ni exponer el JWT HttpOnly. */
@RestController
@SecurityScheme(name="csrf", type=SecuritySchemeType.APIKEY, in=SecuritySchemeIn.HEADER,
        paramName="X-CSRF-TOKEN", description="Inicia sesión en este navegador; consulta GET /api/v1/csrf y pega token aquí. No es el JWT. Renueva tras cambiar de sesión.")
public class CsrfApiController {
    /** Contrato de protección CSRF de la sesión auxiliar; no contiene credenciales. */
    public record ProteccionCsrf(String parametro, String cabecera, String token) { }

    /** Devuelve el token asociado a la sesión del navegador autenticado, sin caché. */
    @Operation(summary="Obtiene CSRF para Swagger", description="Requiere login web previo en el mismo origen HTTPS. Copia token en Authorize/csrf para POST, PUT y DELETE. La cookie de autenticación se envía automáticamente.")
    @GetMapping("/api/v1/csrf")
    public ResponseEntity<ProteccionCsrf> actual(CsrfToken csrf) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new ProteccionCsrf(csrf.getParameterName(),csrf.getHeaderName(),csrf.getToken()));
    }
}
