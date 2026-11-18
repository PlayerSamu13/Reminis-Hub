package dev.reminishub.service;

import dev.reminishub.config.JwtConfig;
import dev.reminishub.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Emite el JWT de sesion y construye su cookie.
 * El identificador único permite revocar una sesión sin guardar su JWT.
 */
@Service
public class TokenService {
    private final JwtEncoder codificador;
    private final UsuarioRepository usuarios;
    private final long minutos;

    public TokenService(JwtEncoder codificador, UsuarioRepository usuarios,
                        @Value("${reminishub.jwt.minutos:60}") long minutos) {
        this.codificador = codificador;
        this.usuarios = usuarios;
        if (minutos < 1 || minutos > 60) {
            throw new IllegalArgumentException("RH_JWT_MINUTOS debe estar entre 1 y 60.");
        }
        this.minutos = minutos;
    }

    /** Vigencia en minutos, util para documentar y probar el contrato de la cookie. */
    public long minutosDeVigencia() { return minutos; }

    /**
     * Emite un token firmado para una cuenta ya autenticada.
     * @param email correo normalizado de la cuenta
     * @return JWT compacto con emisor, asunto, nombre y rol
     */
    public String emitir(String email) {
        var cuenta = usuarios.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("La cuenta desaparecio durante el login."));
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.EMISOR)
                .id(java.util.UUID.randomUUID().toString())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(Duration.ofMinutes(minutos)))
                .subject(cuenta.getEmail())
                .claim("nombre", cuenta.getNombre())
                .claim("rol", cuenta.getRol().name())
                .build();
        return codificador.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    /** HttpOnly impide leer el token desde JavaScript; Secure restringe su envio a HTTPS. */
    public ResponseCookie cookie(String token) {
        return ResponseCookie.from(JwtConfig.COOKIE, token)
                .httpOnly(true).secure(true).sameSite("Lax").path("/")
                .maxAge(Duration.ofMinutes(minutos)).build();
    }

    /** Cookie vacia con la misma ruta y atributos: el navegador la descarta. */
    public ResponseCookie cookieVacia() {
        return ResponseCookie.from(JwtConfig.COOKIE, "")
                .httpOnly(true).secure(true).sameSite("Lax").path("/")
                .maxAge(Duration.ZERO).build();
    }
}
