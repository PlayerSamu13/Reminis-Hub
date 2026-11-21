package dev.reminishub;

import dev.reminishub.config.*;
import dev.reminishub.controller.api.CsrfApiController;
import dev.reminishub.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

/** Contrato de rutas privadas, rol ADMIN y CSRF con la cadena real; los servicios se prueban aparte. */
@WebMvcTest(controllers={SeguridadEscrituraTests.Rutas.class,CsrfApiController.class},
        properties="reminishub.jwt.secret=" + SecretosDePrueba.JWT)
@Import({SecurityConfig.class,JwtConfig.class,SeguridadEscrituraTests.Rutas.class,dev.reminishub.service.TokenService.class})
class SeguridadEscrituraTests {
    @RestController
    static class Rutas {
        @GetMapping("/api/v1/admin/reporte") String reporte(){return "ok";}
        @PostMapping("/api/v1/propuestas") String crear(){return "ok";}
    }
    @Autowired MockMvc mvc;
    @MockitoBean UsuarioRepository usuarios;
    @MockitoBean SesionesRevocadas revocadas;
    @Autowired dev.reminishub.service.TokenService tokens;
    private jakarta.servlet.http.Cookie cuenta(String rol) {
        var usuario=new dev.reminishub.model.entity.Usuario();
        usuario.setEmail("prueba@example.invalid"); usuario.setNombre("Prueba");
        usuario.setRol(dev.reminishub.model.enums.RolUsuario.valueOf(rol));
        org.mockito.Mockito.when(usuarios.findByEmail(usuario.getEmail())).thenReturn(java.util.Optional.of(usuario));
        return new jakarta.servlet.http.Cookie(JwtConfig.COOKIE,tokens.emitir(usuario.getEmail()));
    }

    @Test void apiAnonimaEs401InclusoConAcceptHtml() throws Exception {
        mvc.perform(get("https://localhost/api/v1/admin/reporte").secure(true).accept("text/html"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("https://localhost/api/v1/csrf").secure(true)).andExpect(status().isUnauthorized());
    }
    @Test void colaboradorNoPuedeLeerReporteAdmin() throws Exception {
        mvc.perform(get("https://localhost/api/v1/admin/reporte").secure(true)
                .cookie(cuenta("COLABORADOR")))
                .andExpect(status().isForbidden());
    }
    @Test void adminPuedeLeerReporte() throws Exception {
        mvc.perform(get("https://localhost/api/v1/admin/reporte").secure(true)
                .cookie(cuenta("ADMIN")))
                .andExpect(status().isOk());
    }
    @Test void escrituraExigeCsrfAunAutenticada() throws Exception {
        mvc.perform(post("https://localhost/api/v1/propuestas").secure(true).cookie(cuenta("COLABORADOR")))
                .andExpect(status().isForbidden());
        mvc.perform(post("https://localhost/api/v1/propuestas").secure(true).cookie(cuenta("COLABORADOR")).with(csrf()))
                .andExpect(status().isOk());
    }
    @Test void csrfObtenidoSirveParaEscrituraEnMismaSesion() throws Exception {
        var cookie=cuenta("COLABORADOR");
        var resultado=mvc.perform(get("https://localhost/api/v1/csrf").secure(true).cookie(cookie))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.cabecera").value("X-CSRF-TOKEN")).andReturn();
        String json=resultado.getResponse().getContentAsString();
        var matcher=java.util.regex.Pattern.compile("\"token\":\"([^\"]+)\"").matcher(json);
        org.junit.jupiter.api.Assertions.assertTrue(matcher.find());
        mvc.perform(post("https://localhost/api/v1/propuestas").secure(true).cookie(cookie)
                .session((org.springframework.mock.web.MockHttpSession)resultado.getRequest().getSession(false))
                .header("X-CSRF-TOKEN",matcher.group(1))).andExpect(status().isOk());
    }
}
