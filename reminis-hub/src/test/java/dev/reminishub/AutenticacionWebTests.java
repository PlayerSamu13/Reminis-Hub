package dev.reminishub;

import dev.reminishub.config.JwtConfig;
import dev.reminishub.config.SecurityConfig;
import dev.reminishub.config.WebConfig;
import dev.reminishub.controller.api.SesionApiController;
import dev.reminishub.controller.web.LoginController;
import dev.reminishub.model.entity.Usuario;
import dev.reminishub.model.enums.RolUsuario;
import dev.reminishub.repository.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Login, cookie del JWT y logout contra el filtro de seguridad real, sin base de datos.
 * El emisor y el verificador son los de produccion: la firma del token se comprueba de verdad.
 */
@WebMvcTest(controllers = {LoginController.class, SesionApiController.class},
        properties = "reminishub.jwt.secret=" + SecretosDePrueba.JWT)
@Import({SecurityConfig.class, JwtConfig.class, WebConfig.class, dev.reminishub.service.TokenService.class})
class AutenticacionWebTests {
    private static final String EMAIL = "cuenta@example.invalid";
    private static final String CLAVE = "Frase ficticia para pruebas 123";

    @Autowired MockMvc mvc;
    @Autowired JwtDecoder decodificador;
    @MockitoBean AuthenticationManager autenticador;
    @MockitoBean dev.reminishub.service.IntentosLogin intentos;

    @Test
    void limiteDeIntentosIndicaCuandoReintentar() throws Exception {
        when(intentos.reservar(EMAIL)).thenReturn(32L);
        mvc.perform(post("https://localhost/login").secure(true).with(csrf()).param("email", EMAIL))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "32"))
                .andExpect(content().string(containsString("32 segundos")))
                .andExpect(cookie().doesNotExist(JwtConfig.COOKIE));
        verify(autenticador, never()).authenticate(any());
    }
    @MockitoBean dev.reminishub.repository.SesionesRevocadas revocadas;
    @MockitoBean UsuarioRepository usuarios;

    @BeforeEach
    void prepararCuenta() {
        Usuario cuenta = new Usuario();
        cuenta.setEmail(EMAIL);
        cuenta.setNombre("Cuenta de Prueba");
        cuenta.setPasswordHash("{bcrypt}$2a$12$hashficticio");
        cuenta.setRol(RolUsuario.COLABORADOR);
        cuenta.setActivo(true);
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.of(cuenta));
        when(autenticador.authenticate(any()))
                .thenReturn(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(EMAIL, CLAVE));
    }

    /** Ejecuta el login correcto y devuelve la cookie emitida. */
    private Cookie iniciarSesion() throws Exception {
        MvcResult resultado = mvc.perform(post("https://localhost/login").secure(true).with(csrf())
                        .param("email", EMAIL).param("contrasena", CLAVE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proyectos"))
                .andReturn();
        Cookie cookie = resultado.getResponse().getCookie(JwtConfig.COOKIE);
        assertNotNull(cookie, "El login debe emitir la cookie del JWT");
        return cookie;
    }

    @Test
    void formularioTraducidoSinRepoblarLaClave() throws Exception {
        mvc.perform(get("https://localhost/login").secure(true).param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Iniciar sesión")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("autocomplete=\"username\"")))
                .andExpect(content().string(containsString("autocomplete=\"current-password\"")))
                .andExpect(content().string(containsString("href=\"/registro\"")));
        mvc.perform(get("https://localhost/login").secure(true).param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sign in")))
                .andExpect(content().string(containsString("Create an account")));
    }

    @Test
    void loginCorrectoEmiteCookieHttpOnlySecureConTokenVerificable() throws Exception {
        Cookie cookie = iniciarSesion();
        assertEquals(JwtConfig.COOKIE, cookie.getName());

        Jwt token = decodificador.decode(cookie.getValue());
        assertEquals(EMAIL, token.getSubject());
        assertEquals("COLABORADOR", token.getClaimAsString("rol"));
    }

    @Test
    void laCookieDelLoginViajaConLosAtributosExigidos() throws Exception {
        String cabecera = mvc.perform(post("https://localhost/login").secure(true).with(csrf())
                        .param("email", EMAIL).param("contrasena", CLAVE))
                .andReturn().getResponse().getHeader("Set-Cookie");
        assertNotNull(cabecera);
        assertTrue(cabecera.contains("HttpOnly"), "HttpOnly impide leer el token desde JavaScript: " + cabecera);
        assertTrue(cabecera.contains("SameSite=Lax"), cabecera);
        assertTrue(cabecera.contains("Path=/"), cabecera);
        assertTrue(cabecera.contains("Secure"), "La cookie debe marcarse Secure: " + cabecera);
    }

    @Test
    void credencialesInvalidasNoEmitenCookieNiRepitenLaClave() throws Exception {
        when(autenticador.authenticate(any())).thenThrow(new BadCredentialsException("Credenciales no validas."));
        mvc.perform(post("https://localhost/login").secure(true).with(csrf()).param("lang", "es")
                        .param("email", EMAIL).param("contrasena", CLAVE))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist(JwtConfig.COOKIE))
                .andExpect(content().string(containsString("El correo o la contraseña no son correctos")))
                .andExpect(content().string(not(containsString(CLAVE))));
    }

    @Test
    void loginSinCsrfSeRechaza() throws Exception {
        mvc.perform(post("https://localhost/login").secure(true).param("email", EMAIL).param("contrasena", CLAVE))
                .andExpect(status().isForbidden());
        verify(autenticador, never()).authenticate(any());
    }

    /**
     * Recorrido del navegador: el token CSRF nace en la sesion al abrir el formulario y viaja
     * en el campo oculto. No se usa el atajo {@code with(csrf())}, asi se comprueba que la
     * sesion sigue existiendo pese a la politica STATELESS (que solo afecta al contexto de seguridad).
     */
    @Test
    void elTokenCsrfDelFormularioSeAceptaEnElPost() throws Exception {
        MvcResult formulario = mvc.perform(get("https://localhost/login").secure(true).param("lang", "es"))
                .andExpect(status().isOk())
                .andReturn();
        var sesion = formulario.getRequest().getSession(false);
        assertNotNull(sesion, "El formulario debe abrir sesion para recordar el token CSRF");

        // Se toma el valor realmente impreso en el campo oculto, como lo enviaria el navegador.
        var encontrado = java.util.regex.Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"")
                .matcher(formulario.getResponse().getContentAsString());
        assertTrue(encontrado.find(), "El formulario debe imprimir el campo _csrf");
        String valor = encontrado.group(1);

        mvc.perform(post("https://localhost/login").secure(true)
                        .session((org.springframework.mock.web.MockHttpSession) sesion)
                        .param("_csrf", valor)
                        .param("email", EMAIL).param("contrasena", CLAVE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proyectos"))
                .andExpect(cookie().exists(JwtConfig.COOKIE));
    }

    @Test
    void laSesionExigeQueLaCuentaSigaActivaYConserveSuRol() throws Exception {
        Cookie cookie = iniciarSesion();
        Usuario cuenta = usuarios.findByEmail(EMAIL).orElseThrow();
        cuenta.setActivo(false);
        mvc.perform(get("https://localhost/api/v1/sesion").secure(true).cookie(cookie))
                .andExpect(status().isUnauthorized());
        cuenta.setActivo(true);
        cuenta.setRol(RolUsuario.ADMIN);
        mvc.perform(get("https://localhost/api/v1/sesion").secure(true).cookie(cookie))
                .andExpect(status().isUnauthorized());
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.empty());
        mvc.perform(get("https://localhost/api/v1/sesion").secure(true).cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiSesionSinTokenDevuelve401YNoRedirige() throws Exception {
        mvc.perform(get("https://localhost/api/v1/sesion").secure(true).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiSinTokenMantiene401AunqueElClientePidaHtml() throws Exception {
        mvc.perform(get("https://localhost/api/v1/sesion").secure(true).accept(MediaType.TEXT_HTML))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void errorDeLoginTieneDescripcionYFocoSinRevelarCredenciales() throws Exception {
        when(autenticador.authenticate(any())).thenThrow(new BadCredentialsException("No valido"));
        var resultado = mvc.perform(post("https://localhost/login").secure(true).with(csrf())
                        .param("email", EMAIL).param("contrasena", CLAVE))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("id=\"login-error\"")))
                .andExpect(content().string(not(containsString(CLAVE))))
                .andReturn();
        String html = resultado.getResponse().getContentAsString();
        assertEquals(2, java.util.regex.Pattern.compile("aria-describedby=\"login-error\"").matcher(html).results().count());
        assertEquals(1, java.util.regex.Pattern.compile("\\sautofocus(?:=|\\s|>)").matcher(html).results().count());
        assertTrue(java.util.regex.Pattern.compile("<input[^>]*id=\"email\"[^>]*\\sautofocus(?:=|\\s|>)").matcher(html).find());
    }

    @Test
    void apiSesionConTokenValidoDevuelveIdentidadDelToken() throws Exception {
        Cookie cookie = iniciarSesion();
        mvc.perform(get("https://localhost/api/v1/sesion").secure(true).cookie(cookie)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.nombre").value("Cuenta de Prueba"))
                .andExpect(jsonPath("$.rol").value("COLABORADOR"));
    }

    @Test
    void unTokenAlteradoNoAutentica() throws Exception {
        Cookie falsa = new Cookie(JwtConfig.COOKIE, "token.inventado.firma");
        mvc.perform(get("https://localhost/api/v1/sesion").secure(true).cookie(falsa)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutAutenticadoBorraLaCookie() throws Exception {
        Cookie cookie = iniciarSesion();
        Jwt jwt = decodificador.decode(cookie.getValue());
        var id = java.util.UUID.fromString(jwt.getId());
        mvc.perform(post("https://localhost/logout").secure(true).with(csrf()).cookie(cookie))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?salida"))
                .andExpect(cookie().maxAge(JwtConfig.COOKIE, 0))
                .andExpect(cookie().value(JwtConfig.COOKIE, ""));
        verify(revocadas).revocar(id, jwt.getExpiresAt());
        when(revocadas.contiene(id)).thenReturn(true);
        mvc.perform(get("https://localhost/api/v1/sesion").secure(true).cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutSinSesionRedirigeAlFormulario() throws Exception {
        mvc.perform(post("https://localhost/logout").secure(true).with(csrf())
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(cookie().doesNotExist(JwtConfig.COOKIE));
    }

    /**
     * Sin token CSRF el cierre de sesion no se ejecuta. La validacion ocurre antes de leer el
     * JWT, asi que el servidor responde 403 y la cookie sigue siendo valida.
     */
    @Test
    void logoutSinCsrfNoCierraLaSesion() throws Exception {
        Cookie cookie = iniciarSesion();
        mvc.perform(post("https://localhost/logout").secure(true).cookie(cookie).accept(MediaType.TEXT_HTML))
                .andExpect(status().isForbidden())
                .andExpect(cookie().doesNotExist(JwtConfig.COOKIE));

        // La misma cookie sigue sirviendo: nada se borro en el servidor ni en el cliente.
        mvc.perform(get("https://localhost/api/v1/sesion").secure(true).cookie(cookie)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void conSesionValidaElFormularioRedirigeAlCatalogo() throws Exception {
        Cookie cookie = iniciarSesion();
        mvc.perform(get("https://localhost/login").secure(true).cookie(cookie).param("lang", "es"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proyectos"));
    }
}
