package dev.reminishub.controller.web;

import dev.reminishub.dto.SesionActual;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expone la sesion a las vistas sin consultar la base en cada pagina.
 * Los datos salen de los claims del JWT ya verificado por el filtro de seguridad.
 */
@ControllerAdvice(basePackageClasses = ControllerMVC.class)
public class SesionAdvice {
    @ModelAttribute("sesion")
    public SesionActual sesion(Authentication autenticacion) {
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || !(autenticacion.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }
        return new SesionActual(jwt.getSubject(), jwt.getClaimAsString("nombre"), jwt.getClaimAsString("rol"));
    }
}
