package dev.reminishub;

import dev.reminishub.config.SecurityConfig;
import dev.reminishub.config.WebConfig;
import dev.reminishub.controller.web.RegistroController;
import dev.reminishub.service.CorreoRegistradoException;
import dev.reminishub.service.RegistroService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Contrato web, renderizado y seguridad del formulario, sin conexion a PostgreSQL. */
@WebMvcTest(controllers = RegistroController.class,
        properties = "reminishub.jwt.secret=" + SecretosDePrueba.JWT)
@Import({SecurityConfig.class, WebConfig.class, dev.reminishub.config.JwtConfig.class})
class RegistroWebTests {
    @Autowired MockMvc mvc;
    @MockitoBean RegistroService registro;
    @MockitoBean dev.reminishub.service.IntentosRegistro intentos;

    @Test
    void limiteUsaIpDeConexionYNoCabeceraDelCliente() throws Exception {
        when(intentos.reservar("127.0.0.1")).thenReturn(40L);
        for (String idioma : java.util.List.of("es", "en", "eo")) {
            String espera = switch (idioma) {
                case "es" -> "40 segundos";
                case "en" -> "40 seconds";
                default -> "40 sekundoj";
            };
            mvc.perform(post("https://localhost/registro").secure(true).with(csrf())
                            .header("X-Forwarded-For", "203.0.113.9")
                            .cookie(new jakarta.servlet.http.Cookie("reminishub_lang", idioma))
                            .param("nombre", "Cuenta prueba").param("email", "registro@example.invalid")
                            .param("contrasena", "Frase ficticia para pruebas 123"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().string("Retry-After", "40"))
                    .andExpect(content().string(containsString(espera)))
                    .andExpect(content().string(not(containsString("Frase ficticia para pruebas 123"))));
        }
        verifyNoInteractions(registro);
        verify(intentos, times(3)).reservar("127.0.0.1");
    }
    @MockitoBean dev.reminishub.repository.SesionesRevocadas revocadas;
    @MockitoBean dev.reminishub.repository.UsuarioRepository usuarios;

    @Test
    void formularioTraducidoIncluyeCsrf() throws Exception {
        mvc.perform(get("https://localhost/registro").secure(true).param("lang", "es"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Nombre público")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("https://localhost/registro").secure(true).param("lang", "en"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Public name")));
        mvc.perform(get("https://localhost/registro").secure(true).param("lang", "eo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"eo\"")))
                .andExpect(content().string(containsString("Publika nomo")));
    }

    @Test
    void datosInvalidosNoLleganAlServicioNiRepoblanClave() throws Exception {
        mvc.perform(post("https://localhost/registro").secure(true).with(csrf()).cookie(new jakarta.servlet.http.Cookie("reminishub_lang", "en"))
                        .param("nombre", "X").param("email", "invalido").param("contrasena", "CLAVE_CORTA"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Enter a name")))
                .andExpect(content().string(not(containsString("CLAVE_CORTA"))));
        verifyNoInteractions(registro);
    }

    @Test
    void registroValidoRedirigeSinDatosPersonales() throws Exception {
        mvc.perform(post("https://localhost/registro").secure(true).with(csrf()).param("nombre", "Cuenta prueba")
                        .param("email", "registro@example.invalid").param("contrasena", "Frase ficticia para pruebas 123"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/registro"))
                .andExpect(flash().attribute("registroCreado", true));
        verify(registro).registrar(any());
    }

    @Test
    void correoDuplicadoDevuelve409Traducido() throws Exception {
        doThrow(new CorreoRegistradoException()).when(registro).registrar(any());
        mvc.perform(post("https://localhost/registro").secure(true).with(csrf()).cookie(new jakarta.servlet.http.Cookie("reminishub_lang", "en"))
                        .param("nombre", "Cuenta prueba").param("email", "registro@example.invalid")
                        .param("contrasena", "Frase ficticia para pruebas 123"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("An account already uses that email address.")))
                .andExpect(content().string(not(containsString("Frase ficticia para pruebas 123"))));
    }

    @Test
    void formularioLimpioNoReferenciaErroresInexistentes() throws Exception {
        mvc.perform(get("https://localhost/registro").secure(true).param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-describedby=\"clave-ayuda\"")))
                .andExpect(content().string(not(containsString("aria-describedby=\"nombre-error\""))))
                .andExpect(content().string(not(containsString("aria-describedby=\"email-error\""))))
                .andExpect(content().string(not(containsString("clave-bytes-error\""))))
                .andExpect(content().string(not(containsString("is-invalid"))));
    }

    @Test
    void erroresMarcanElCampoYSoloReferencianMensajesExistentes() throws Exception {
        mvc.perform(post("https://localhost/registro").secure(true).with(csrf()).param("lang", "es")
                        .param("nombre", "X").param("email", "invalido").param("contrasena", "CLAVE_CORTA"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("id=\"nombre-error\"")))
                .andExpect(content().string(containsString("id=\"email-error\"")))
                .andExpect(content().string(containsString("aria-describedby=\"nombre-error\"")))
                .andExpect(content().string(containsString("aria-describedby=\"email-error\"")))
                .andExpect(content().string(containsString("aria-describedby=\"clave-ayuda clave-error\"")))
                .andExpect(content().string(containsString("is-invalid")))
                .andExpect(content().string(containsString("invalid-feedback")));
    }

    /** El foco automatico solo puede aparecer una vez y solo en el primer campo invalido. */
    private static String htmlDe(org.springframework.test.web.servlet.MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static long vecesAutofocus(String html) {
        return java.util.regex.Pattern.compile("\\bautofocus=").matcher(html).results().count();
    }

    private static boolean enfoca(String html, String id) {
        return java.util.regex.Pattern.compile("(?s)<input[^>]*\\bid=\"" + id + "\"[^>]*\\bautofocus=")
                .matcher(html).find();
    }

    @Test
    void focoVaAlPrimerCampoInvalidoEnOrdenNombreEmailClave() throws Exception {
        var r = mvc.perform(post("https://localhost/registro").secure(true).with(csrf()).param("lang", "es")
                        .param("nombre", "X").param("email", "invalido").param("contrasena", "CLAVE_CORTA"))
                .andExpect(status().isBadRequest()).andReturn();
        String html = htmlDe(r);
        org.junit.jupiter.api.Assertions.assertEquals(1, vecesAutofocus(html), "solo un autofocus");
        org.junit.jupiter.api.Assertions.assertTrue(enfoca(html, "nombre"), "el foco corresponde al nombre");
        verifyNoInteractions(registro);
    }

    @Test
    void focoApuntaAlCorreoCuandoElNombreEsValido() throws Exception {
        var r = mvc.perform(post("https://localhost/registro").secure(true).with(csrf()).param("lang", "es")
                        .param("nombre", "Cuenta prueba").param("email", "invalido")
                        .param("contrasena", "Frase ficticia para pruebas 123"))
                .andExpect(status().isBadRequest()).andReturn();
        String html = htmlDe(r);
        org.junit.jupiter.api.Assertions.assertEquals(1, vecesAutofocus(html));
        org.junit.jupiter.api.Assertions.assertTrue(enfoca(html, "email"));
    }

    @Test
    void focoApuntaALaClavePorLongitudYPorBytes() throws Exception {
        var corta = mvc.perform(post("https://localhost/registro").secure(true).with(csrf()).param("lang", "es")
                        .param("nombre", "Cuenta prueba").param("email", "registro@example.invalid")
                        .param("contrasena", "CLAVE_CORTA"))
                .andExpect(status().isBadRequest()).andReturn();
        org.junit.jupiter.api.Assertions.assertTrue(enfoca(htmlDe(corta), "contrasena"));

        var bytes = mvc.perform(post("https://localhost/registro").secure(true).with(csrf()).param("lang", "es")
                        .param("nombre", "Cuenta prueba").param("email", "registro@example.invalid")
                        .param("contrasena", "\uD83D\uDE00".repeat(20)))
                .andExpect(status().isBadRequest()).andReturn();
        String htmlBytes = htmlDe(bytes);
        org.junit.jupiter.api.Assertions.assertEquals(1, vecesAutofocus(htmlBytes),
                "el error de bytes tambien debe recibir el foco");
        org.junit.jupiter.api.Assertions.assertTrue(enfoca(htmlBytes, "contrasena"));
        org.junit.jupiter.api.Assertions.assertTrue(htmlBytes.contains("aria-describedby=\"clave-ayuda clave-bytes-error\""));
        verifyNoInteractions(registro);
    }

    @Test
    void formularioLimpioNoRobaElFoco() throws Exception {
        var r = mvc.perform(get("https://localhost/registro").secure(true).param("lang", "es"))
                .andExpect(status().isOk()).andReturn();
        org.junit.jupiter.api.Assertions.assertEquals(0, vecesAutofocus(htmlDe(r)));
    }

    @Test
    void postSinCsrfSeRechaza() throws Exception {
        mvc.perform(post("https://localhost/registro").secure(true)).andExpect(status().isForbidden());
        verifyNoInteractions(registro);
    }

    @Test
    void formularioHttpRedirigeAntesDeRecibirCredenciales() throws Exception {
        mvc.perform(get("/registro").with(request -> { request.setServerPort(8080); return request; }))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://localhost:8443/registro"));
        verifyNoInteractions(registro);
    }
}
