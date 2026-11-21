package dev.reminishub;

import dev.reminishub.config.JwtConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Recorrido completo contra PostgreSQL real: registro, login con BCrypt, uso del JWT en la
 * cookie, logout y rechazo posterior. La cuenta se borra al terminar; no toca las semillas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "reminishub.jwt.secret=" + SecretosDePrueba.JWT)
@EnabledIfEnvironmentVariable(named = "CATALOGO_DB_TESTS", matches = "true")
class SesionE2ETests {
    private static final java.util.concurrent.atomic.AtomicInteger CLIENTE =
            new java.util.concurrent.atomic.AtomicInteger();
    // IP documental simulada: cada caso conserva el limitador real sin heredar intentos ajenos.
    private final String ipDePrueba = "192.0.2." + CLIENTE.incrementAndGet();
    private static final String PREFIJO = "e2e-sesion-";
    private static final String CLAVE = "Frase ficticia para sesion E2E 123";

    // JUnit crea una instancia por prueba: solo se limpia su cuenta exacta.
    private final String emailDePrueba = PREFIJO + UUID.randomUUID() + "@example.invalid";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void limpiarCuentasDePrueba() {
        jdbc.update("DELETE FROM usuario WHERE email = ?", emailDePrueba);
    }

    /** Registra la cuenta por el formulario real y devuelve la cookie del login. */
    private Cookie entrar(String email, String nombre) throws Exception {
        mvc.perform(post("https://localhost/registro").secure(true).with(csrf())
                        .with(peticion -> { peticion.setRemoteAddr(ipDePrueba); return peticion; })
                        .param("nombre", nombre).param("email", email).param("contrasena", CLAVE))
                .andExpect(status().is3xxRedirection());

        MvcResult acceso = mvc.perform(post("https://localhost/login").secure(true).with(csrf())
                        .param("email", email).param("contrasena", CLAVE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proyectos"))
                .andReturn();
        Cookie cookie = acceso.getResponse().getCookie(JwtConfig.COOKIE);
        assertNotNull(cookie, "El login debe emitir la cookie del JWT");
        assertTrue(cookie.isHttpOnly(), "La cookie no puede quedar accesible a JavaScript");
        return cookie;
    }

    @Test
    void registroLoginUsoYLogoutConPostgresReal() throws Exception {
        String email = emailDePrueba;
        String nombre = "Cuenta Sesion E2E";

        Cookie cookie = entrar(email, nombre);

        // La API confirma identidad y rol leidos del token, no de parametros.
        mvc.perform(get("https://localhost/api/v1/sesion").secure(true).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.nombre").value(nombre))
                .andExpect(jsonPath("$.rol").value("COLABORADOR"));

        // La misma cookie se traduce en la cabecera de la vista publica.
        mvc.perform(get("https://localhost/proyectos").secure(true).cookie(cookie).param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hola, " + nombre)))
                .andExpect(content().string(not(containsString("Iniciar sesión"))));

        // Sin CSRF no se cierra la sesion y la cookie sigue sirviendo.
        mvc.perform(post("https://localhost/logout").secure(true).cookie(cookie))
                .andExpect(status().isForbidden());
        mvc.perform(get("https://localhost/api/v1/sesion").secure(true).cookie(cookie))
                .andExpect(status().isOk());

        // Con CSRF, el servidor ordena al navegador borrar la cookie.
        mvc.perform(post("https://localhost/logout").secure(true).with(csrf()).cookie(cookie))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?salida"))
                .andExpect(cookie().maxAge(JwtConfig.COOKIE, 0));

        // El formulario confirma la salida con el mensaje de un solo uso.
        mvc.perform(get("https://localhost/login").secure(true).param("salida", "").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Has cerrado la sesión.")));
    }

    @Test
    void elLogoutRevocaSoloLaSesionCerrada() throws Exception {
        String email = emailDePrueba;
        Cookie cookie = entrar(email, "Cuenta Copia E2E");
        Cookie segunda = mvc.perform(post("https://localhost/login").secure(true).with(csrf())
                        .param("email", email).param("contrasena", CLAVE))
                .andReturn().getResponse().getCookie(JwtConfig.COOKIE);
        assertNotNull(segunda);

        mvc.perform(post("https://localhost/logout").secure(true).with(csrf()).cookie(cookie))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("https://localhost/api/v1/sesion").secure(true).cookie(cookie))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("https://localhost/api/v1/sesion").secure(true).cookie(segunda))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void unaCuentaInactivaNoPuedeIniciarSesionAunqueLaClaveSeaCorrecta() throws Exception {
        String email = emailDePrueba;
        mvc.perform(post("https://localhost/registro").secure(true).with(csrf())
                        .with(peticion -> { peticion.setRemoteAddr(ipDePrueba); return peticion; })
                        .param("nombre", "Cuenta Inactiva").param("email", email).param("contrasena", CLAVE))
                .andExpect(status().is3xxRedirection());
        jdbc.update("UPDATE usuario SET activo = false WHERE email = ?", email);

        mvc.perform(post("https://localhost/login").secure(true).with(csrf())
                        .param("email", email).param("contrasena", CLAVE))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist(JwtConfig.COOKIE));
    }
}
