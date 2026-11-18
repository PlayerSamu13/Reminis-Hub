package dev.reminishub;

import dev.reminishub.dto.RegistroForm;
import dev.reminishub.repository.UsuarioRepository;
import dev.reminishub.service.CorreoRegistradoException;
import dev.reminishub.service.RegistroService;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** PostgreSQL real. Cada prueba revierte sus cuentas y no modifica los ejemplos existentes. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "reminishub.jwt.secret=" + SecretosDePrueba.JWT)
@EnabledIfEnvironmentVariable(named = "CATALOGO_DB_TESTS", matches = "true")
class RegistroIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired RegistroService registro;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired Validator validator;

    String correo() { return "registro" + UUID.randomUUID().toString().replace("-", "") + "@example.invalid"; }

    @Test
    void formularioPersisteHashYCorreoNormalizado() throws Exception {
        String email = correo();
        String clave = "Frase ficticia para pruebas 123";
        mvc.perform(post("https://localhost/registro").secure(true).with(csrf()).param("nombre", "  Cuenta ficticia  ")
                        .param("email", " " + email.toUpperCase(java.util.Locale.ROOT) + " ").param("contrasena", clave))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/registro"));
        var hash = jdbc.queryForObject("SELECT password_hash FROM usuario WHERE email=?", String.class, email);
        assertNotEquals(clave, hash);
        assertTrue(encoder.matches(clave, hash));
        assertEquals("Cuenta ficticia", jdbc.queryForObject("SELECT nombre FROM usuario WHERE email=?", String.class, email));
    }

    @Test
    void correoExistenteTrasNormalizarSeRechaza() {
        String email = correo();
        jdbc.update("INSERT INTO usuario(email,nombre) VALUES (?,?)", email, "Cuenta ficticia");
        var form = RegistroServiceTests.formulario();
        form.setEmail(" " + email.toUpperCase(java.util.Locale.ROOT) + " ");
        assertThrows(CorreoRegistradoException.class, () -> registro.registrar(form));
    }

    @Test
    void restriccionRealResuelveConsultaPreviaDesactualizada() {
        String email = correo();
        jdbc.update("INSERT INTO usuario(email,nombre) VALUES (?,?)", email, "Cuenta ficticia");
        // Simula un resultado de existencia desactualizado; el INSERT y su excepcion son reales.
        var consultaDesactualizada = mock(UsuarioRepository.class);
        when(consultaDesactualizada.existsByEmail(email)).thenReturn(false);
        when(consultaDesactualizada.saveAndFlush(any())).thenAnswer(call -> usuarios.saveAndFlush(call.getArgument(0)));
        var servicio = new RegistroService(consultaDesactualizada, encoder, validator);
        RegistroForm form = RegistroServiceTests.formulario();
        form.setEmail(email);
        assertThrows(CorreoRegistradoException.class, () -> servicio.registrar(form));
        // La transaccion de esta prueba se revierte tras el error SQL; no se consulta de nuevo.
    }

    // Tu turno: POST /registro con .secure(true), .with(csrf()), datos validos y
    // .param("rol", "ADMIN"). Espera redireccion y consulta por el correo unico:
    // SELECT rol FROM usuario WHERE email=? debe devolver COLABORADOR.
    @Test
    void validacionRolBaseDatos() throws Exception {
        String email = correo();
        String clave = "Frase ficticia para pruebas 123";
        mvc.perform(post("https://localhost/registro").secure(true).with(csrf())
                .param("rol", "ADMIN")
                .param("nombre", " Cuenta Ficticia ")
                .param("email", " " + email.toUpperCase(Locale.ROOT) + " ")
                .param("contrasena", clave))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registro"));
        var rol = jdbc.queryForObject("SELECT rol FROM usuario WHERE email=?", String.class, email);
        assertEquals("COLABORADOR", rol);
    }
}
