package dev.reminishub;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 * Recorrido completo del registro contra PostgreSQL real, sin transaccion de prueba.
 * La peticion confirma; la comprobacion posterior abre otra transaccion, de modo que
 * demuestra durabilidad tras el commit y el caracter de un solo uso del mensaje flash.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "reminishub.jwt.secret=" + SecretosDePrueba.JWT)
@EnabledIfEnvironmentVariable(named = "CATALOGO_DB_TESTS", matches = "true")
class RegistroE2ETests {
    private static final String PREFIJO = "e2e-registro-";

    // JUnit crea una instancia por prueba: solo se limpia su cuenta exacta.
    private final String emailDePrueba = PREFIJO + UUID.randomUUID() + "@example.invalid";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void limpiarCuentasDePrueba() {
        jdbc.update("DELETE FROM usuario WHERE email = ?", emailDePrueba);
    }

    @Test
    void altaConfirmaEnElSiguienteGetYConservaLaFilaTrasCommit() throws Exception {
        String email = emailDePrueba;

        var resultado = mvc.perform(post("https://localhost/registro").secure(true).with(csrf())
                        .param("nombre", "Cuenta E2E")
                        .param("email", email)
                        .param("contrasena", "Frase ficticia para E2E 123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registro"))
                .andReturn();

        // El GET posterior con la misma sesion consume el flash de un solo uso.
        var sesion = (MockHttpSession) resultado.getRequest().getSession(false);
        assertNotNull(sesion, "La redireccion debe conservar la sesion con el mensaje flash");
        mvc.perform(get("https://localhost/registro").secure(true).session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tu cuenta de colaborador se ha creado.")));

        // Una segunda visita no repite la confirmacion.
        mvc.perform(get("https://localhost/registro").secure(true).session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Tu cuenta de colaborador se ha creado."))));

        // La fila existe fuera de la transaccion de la peticion: el commit ocurrio.
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM usuario WHERE email = ?", Integer.class, email));
        assertEquals("COLABORADOR", jdbc.queryForObject(
                "SELECT rol FROM usuario WHERE email = ?", String.class, email));
    }
}
