package dev.reminishub.controller.web;

import dev.reminishub.service.TokenService;
import dev.reminishub.service.IntentosLogin;
import dev.reminishub.repository.SesionesRevocadas;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;

/**
 * Login y logout sobre HTTPS.
 * El formulario valida credenciales contra BCrypt y, si son correctas, emite un JWT en cookie
 * HttpOnly. Ninguna credencial se registra en logs ni se devuelve a la vista.
 */
@Controller
public class LoginController {
    @org.springframework.web.bind.annotation.ModelAttribute("googleHabilitado")
    boolean googleHabilitado(@org.springframework.beans.factory.annotation.Value("${reminishub.google.habilitado:false}") boolean habilitado) {
        return habilitado;
    }
    private final AuthenticationManager autenticador;
    private final TokenService tokens;

    private final SesionesRevocadas revocadas;

    private final IntentosLogin intentos;

    public LoginController(AuthenticationManager autenticador, TokenService tokens, SesionesRevocadas revocadas,
                           IntentosLogin intentos) {
        this.intentos = intentos;
        this.revocadas = revocadas;
        this.autenticador = autenticador;
        this.tokens = tokens;
    }

    /** Formulario de acceso; si ya hay sesion valida no se vuelve a pedir la clave. */
    @GetMapping("/login")
    public String formulario(Authentication autenticacion,
                             @RequestParam(name = "salida", required = false) String salida,
                             Model modelo) {
        if (autenticacion != null && autenticacion.isAuthenticated()
                && autenticacion.getPrincipal() instanceof Jwt) {
            return "redirect:/proyectos";
        }
        if (salida != null) {
            modelo.addAttribute("salida", true);
        }
        return "login";
    }

    /**
     * Autentica y responde con la cookie del JWT.
     * Un correo inexistente, una clave incorrecta y una cuenta inactiva producen la misma
     * respuesta: no se revela que cuentas existen.
     */
    @PostMapping("/login")
    public String entrar(@RequestParam(name = "email", required = false) String email,
                         @RequestParam(name = "contrasena", required = false) String contrasena,
                         HttpServletResponse respuesta, Model modelo) {
        String correo = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
        modelo.addAttribute("correoLogin", correo.length() <= 254 ? correo : "");
        long espera = intentos.reservar(correo.length() <= 254 ? correo : "");
        if (espera > 0) {
            respuesta.setStatus(429);
            respuesta.setHeader("Retry-After", Long.toString(espera));
            modelo.addAttribute("esperaLogin", espera);
            return "login";
        }
        try {
            var credencial = new UsernamePasswordAuthenticationToken(
                    correo,
                    contrasena == null ? "" : contrasena);
            var autenticada = autenticador.authenticate(credencial);
            respuesta.addHeader(HttpHeaders.SET_COOKIE, tokens.cookie(tokens.emitir(autenticada.getName())).toString());
            return "redirect:/proyectos";
        } catch (org.springframework.dao.DataAccessException | org.springframework.security.oauth2.jwt.JwtEncodingException excepcion) {
            modelo.addAttribute("falloAcceso", true);
            respuesta.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return "login";
        } catch (AuthenticationException excepcion) {
            modelo.addAttribute("credencialesInvalidas", true);
            respuesta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return "login";
        }
    }

    /** POST con CSRF obligatorio: un enlace GET no puede cerrar la sesion de otro usuario. */
    @PostMapping("/logout")
    public String salir(Authentication autenticacion, HttpServletResponse respuesta,
                        jakarta.servlet.http.HttpServletRequest peticion) {
        Jwt jwt = (Jwt) autenticacion.getPrincipal();
        revocadas.revocar(java.util.UUID.fromString(jwt.getId()), jwt.getExpiresAt());
        var sesion = peticion.getSession(false);
        if (sesion != null) sesion.invalidate();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        respuesta.addHeader(HttpHeaders.SET_COOKIE, tokens.cookieVacia().toString());
        return "redirect:/login?salida";
    }
}
