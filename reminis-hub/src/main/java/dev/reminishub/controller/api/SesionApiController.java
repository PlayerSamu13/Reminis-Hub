package dev.reminishub.controller.api;

import dev.reminishub.dto.SesionActual;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Comprobacion autenticada de la sesion.
 * Sin cookie valida responde 401; el rol proviene del token verificado, no de un parametro.
 */
@RestController
@RequestMapping("/api/v1/sesion")
public class SesionApiController {
    @GetMapping
    public SesionActual actual(Authentication autenticacion) {
        Jwt jwt = (Jwt) autenticacion.getPrincipal();
        return new SesionActual(jwt.getSubject(), jwt.getClaimAsString("nombre"), jwt.getClaimAsString("rol"));
    }
}
