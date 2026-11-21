package dev.reminishub;

import dev.reminishub.config.JwtConfig;
import dev.reminishub.model.entity.Usuario;
import dev.reminishub.model.enums.RolUsuario;
import dev.reminishub.repository.UsuarioRepository;
import dev.reminishub.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Firma, vigencia y autorizaciones del JWT propio, sin contexto Spring ni base de datos. */
class TokenServiceTests {
    private static final String SECRETO = "secreto-solo-para-pruebas-con-mas-de-32-bytes-1234567890";
    private static final String OTRO_SECRETO = "otro-secreto-igual-de-largo-pero-distinto-0987654321xx";

    private JwtEncoder codificador;
    private JwtDecoder decodificador;
    private UsuarioRepository usuarios;
    private TokenService tokens;

    @Test
    void falloRealDelCodificadorDevuelve503SinCookie() {
        JwtEncoder fallido = mock(JwtEncoder.class);
        when(fallido.encode(any())).thenThrow(new org.springframework.security.oauth2.jwt.JwtEncodingException("detalle privado"));
        var emisor = new TokenService(fallido, usuarios, 60);
        var autenticador = mock(org.springframework.security.authentication.AuthenticationManager.class);
        when(autenticador.authenticate(any())).thenReturn(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("cuenta@example.invalid", ""));
        var controlador = new dev.reminishub.controller.web.LoginController(autenticador, emisor,
                mock(dev.reminishub.repository.SesionesRevocadas.class), new dev.reminishub.service.IntentosLogin());
        var respuesta = new org.springframework.mock.web.MockHttpServletResponse();
        var modelo = new org.springframework.ui.ExtendedModelMap();
        assertEquals("login", controlador.entrar("cuenta@example.invalid", "clave", respuesta, modelo));
        assertEquals(503, respuesta.getStatus());
        assertNull(respuesta.getHeader("Set-Cookie"));
        assertEquals(true, modelo.get("falloAcceso"));
        verify(fallido).encode(any());
    }

    @Test
    void cuentaAusenteOInactivaNoPuedeEmitirToken() {
        when(usuarios.findByEmail("cuenta@example.invalid")).thenReturn(Optional.empty());
        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> tokens.emitir("cuenta@example.invalid"));
        var inactiva = cuenta();
        inactiva.setActivo(false);
        when(usuarios.findByEmail("cuenta@example.invalid")).thenReturn(Optional.of(inactiva));
        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> tokens.emitir("cuenta@example.invalid"));
    }

    @BeforeEach
    void preparar() {
        JwtConfig configuracion = new JwtConfig();
        SecretKey clave = configuracion.claveJwt(SECRETO);
        codificador = configuracion.jwtEncoder(clave);
        decodificador = configuracion.jwtDecoder(clave);
        usuarios = mock(UsuarioRepository.class);
        tokens = new TokenService(codificador, usuarios, 60);
        when(usuarios.findByEmail("cuenta@example.invalid")).thenReturn(Optional.of(cuenta()));
    }

    private static Usuario cuenta() {
        Usuario usuario = new Usuario();
        usuario.setEmail("cuenta@example.invalid");
        usuario.setNombre("Cuenta de Prueba");
        usuario.setPasswordHash("{bcrypt}$2a$12$hashficticio");
        usuario.setRol(RolUsuario.ADMIN);
        usuario.setActivo(true);
        usuario.setActivo(true);
        return usuario;
    }

    @Test
    void elTokenLlevaIdentidadRolYVigencia() {
        Jwt token = decodificador.decode(tokens.emitir("cuenta@example.invalid"));

        assertEquals("reminishub", token.getClaimAsString("iss"));
        assertEquals("cuenta@example.invalid", token.getSubject());
        assertEquals("Cuenta de Prueba", token.getClaimAsString("nombre"));
        assertEquals("ADMIN", token.getClaimAsString("rol"));
        Duration vigencia = Duration.between(token.getIssuedAt(), token.getExpiresAt());
        assertEquals(Duration.ofMinutes(60), vigencia);
    }

    @Test
    void unTokenFirmadoConOtraClaveSeRechaza() {
        JwtConfig configuracion = new JwtConfig();
        SecretKey otraClave = configuracion.claveJwt(OTRO_SECRETO);
        String ajeno = configuracion.jwtEncoder(otraClave).encode(
                org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                        org.springframework.security.oauth2.jwt.JwsHeader.with(
                                org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(),
                        org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                                .issuer("reminishub").subject("cuenta@example.invalid")
                                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600))
                                .claim("rol", "ADMIN").build())).getTokenValue();

        assertThrows(JwtException.class, () -> decodificador.decode(ajeno),
                "La firma es lo que impide fabricar un token con rol propio");
    }

    @Test
    void unTokenExpiradoSeRechaza() {
        Instant ahora = Instant.now();
        String vencido = codificador.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                        org.springframework.security.oauth2.jwt.JwsHeader.with(
                                org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(),
                        org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                                .issuer("reminishub").subject("cuenta@example.invalid")
                                .issuedAt(ahora.minusSeconds(7200)).expiresAt(ahora.minusSeconds(3600))
                                .claim("rol", "ADMIN").build()))
                .getTokenValue();

        assertThrows(JwtException.class, () -> decodificador.decode(vencido));
    }

    @Test
    void soloLosRolesDelDominioConcedenAutoridad() {
        var conversor = new JwtConfig().jwtAuthenticationConverter();
        Jwt valido = Jwt.withTokenValue("t").header("alg", "HS256").subject("cuenta@example.invalid")
                .claim("rol", "ADMIN").build();
        Jwt inventado = Jwt.withTokenValue("t").header("alg", "HS256").subject("cuenta@example.invalid")
                .claim("rol", "SUPERADMIN").build();

        var autoridades = conversor.convert(valido).getAuthorities();
        var roles = autoridades.stream().map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .filter(autoridad -> autoridad.startsWith("ROLE_")).toList();
        // Spring Security 7 agrega ademas FACTOR_BEARER; se comprueba solo el rol de dominio.
        assertEquals(1, roles.size(), autoridades.toString());
        assertEquals("ROLE_ADMIN", roles.get(0));
        assertTrue(conversor.convert(inventado).getAuthorities().stream()
                        .noneMatch(autoridad -> autoridad.getAuthority().startsWith("ROLE_")),
                "Un rol inventado no debe conceder permisos");
    }

    @Test
    void laCookieEsHttpOnlySecureYSameSiteLax() {
        ResponseCookie cookie = tokens.cookie("token-de-prueba");

        assertEquals(JwtConfig.COOKIE, cookie.getName());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("Lax", cookie.getSameSite());
        assertEquals("/", cookie.getPath());
        assertEquals(Duration.ofMinutes(60).getSeconds(), cookie.getMaxAge().getSeconds());
        ResponseCookie vacia = tokens.cookieVacia();
        assertEquals(0, vacia.getMaxAge().getSeconds());
        assertEquals("", vacia.getValue());
    }

    @Test
    void laVigenciaConfiguradaDebeEstarEntreUnoYSesentaMinutos() {
        assertThrows(IllegalArgumentException.class, () -> new TokenService(codificador, usuarios, 0));
        assertThrows(IllegalArgumentException.class, () -> new TokenService(codificador, usuarios, 61));
        assertEquals(60, new TokenService(codificador, usuarios, 1).cookie("prueba").getMaxAge().getSeconds());
    }

    @Test
    void unSecretoCortoImpideArrancar() {
        JwtConfig configuracion = new JwtConfig();
        assertThrows(IllegalStateException.class, () -> configuracion.claveJwt("corto"));
        assertThrows(IllegalStateException.class, () -> configuracion.claveJwt(""));
        assertEquals(32, configuracion.claveJwt("12345678901234567890123456789012")
                .getEncoded().length);
    }
}
