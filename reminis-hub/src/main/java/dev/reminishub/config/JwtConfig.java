package dev.reminishub.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import dev.reminishub.model.enums.RolUsuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Firma y verificacion del JWT propio con HS256.
 * El secreto llega por variable de entorno, fuera de Git; el JWT viaja en una cookie HttpOnly
 * y nunca se lee desde JavaScript. Se usa Nimbus, ya presente por el starter de OAuth2.
 */
@Configuration
public class JwtConfig {
    /** Nombre de la cookie que transporta el JWT. */
    public static final String COOKIE = "RH_TOKEN";
    /** Emisor propio. No se emite ni se valida un claim de audiencia en esta etapa. */
    public static final String EMISOR = "reminishub";

    @Bean
    public SecretKey claveJwt(@Value("${reminishub.jwt.secret:}") String secreto) {
        byte[] bytes = secreto == null ? new byte[0] : secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "Configura RH_JWT_SECRET con al menos 32 bytes UTF-8 antes de arrancar; no la guardes en Git.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey claveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey claveJwt) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(claveJwt)
                .macAlgorithm(MacAlgorithm.HS256).build();
        // Valida firma, expiracion y emisor: no basta con que el token se pueda leer.
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISOR));
        return decodificador;
    }

    /**
     * Traduce el claim {@code rol} a una autoridad de Spring.
     * Solo se admiten valores del enum del dominio: un rol inventado en el token no concede permisos.
     */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(jwt -> {
            String rol = jwt.getClaimAsString("rol");
            for (RolUsuario admitido : RolUsuario.values()) {
                if (admitido.name().equals(rol)) {
                    return List.of(new SimpleGrantedAuthority("ROLE_" + admitido.name()));
                }
            }
            return List.of();
        });
        return conversor;
    }
}
