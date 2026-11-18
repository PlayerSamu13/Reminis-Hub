package dev.reminishub.config;

import jakarta.servlet.FilterChain;
import dev.reminishub.repository.UsuarioRepository;
import dev.reminishub.repository.SesionesRevocadas;
import java.util.UUID;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Autentica la peticion con el JWT que viaja en la cookie.
 *
 * Se coloca despues de CsrfFilter a proposito: la validacion CSRF ocurre antes de
 * autenticar, de modo que un POST sin token nunca se ejecuta. El filtro estandar de
 * OAuth2 exime de CSRF las peticiones con bearer token porque supone que viaja en una
 * cabecera; con cookie esa suposicion es falsa y por eso no se usa ese filtro.
 */
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {
    private final JwtDecoder decodificador;
    private final UsuarioRepository usuarios;
    private final SesionesRevocadas revocadas;
    private final Converter<Jwt, AbstractAuthenticationToken> conversor;

    public JwtCookieAuthenticationFilter(JwtDecoder decodificador,
                                         Converter<Jwt, AbstractAuthenticationToken> conversor,
                                         UsuarioRepository usuarios, SesionesRevocadas revocadas) {
        this.revocadas = revocadas;
        this.usuarios = usuarios;
        this.decodificador = decodificador;
        this.conversor = conversor;
    }

    /** Devuelve el valor de la cookie de sesion, o {@code null} si no viene. */
    private static String tokenDeLaCookie(HttpServletRequest peticion) {
        Cookie[] cookies = peticion.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (JwtConfig.COOKIE.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
        String token = tokenDeLaCookie(peticion);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Jwt jwt = decodificador.decode(token);
                if (jwt.getId() != null && jwt.getExpiresAt() != null
                        && !revocadas.contiene(UUID.fromString(jwt.getId())) && cuentaVigente(jwt)) {
                    var autenticacion = conversor.convert(jwt);
                    autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(peticion));
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                }
            } catch (JwtException | IllegalArgumentException excepcion) {
                // Token vencido, alterado o de otro emisor: la peticion sigue como anonima.
                SecurityContextHolder.clearContext();
            }
        }
        cadena.doFilter(peticion, respuesta);
    }

    private boolean cuentaVigente(Jwt jwt) {
        String email = jwt.getSubject();
        return email != null && usuarios.findByEmail(email)
                .filter(cuenta -> Boolean.TRUE.equals(cuenta.getActivo()))
                .filter(cuenta -> cuenta.getRol().name().equals(jwt.getClaimAsString("rol")))
                .isPresent();
    }
}
