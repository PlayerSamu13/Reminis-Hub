package dev.reminishub;

import dev.reminishub.config.JwtConfig;
import dev.reminishub.config.SecurityConfig;
import dev.reminishub.config.WebConfig;
import dev.reminishub.controller.web.ControllerMVC;
import dev.reminishub.dto.PaginaProyectos;
import dev.reminishub.service.CatalogoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Renderizado real del catálogo en los tres idiomas, sin PostgreSQL. */
@WebMvcTest(controllers = ControllerMVC.class,
        properties = "reminishub.jwt.secret=" + SecretosDePrueba.JWT)
@Import({SecurityConfig.class, JwtConfig.class, WebConfig.class})
class CatalogoWebTests {
    @Autowired MockMvc mvc;
    @MockitoBean CatalogoService catalogo;
    @MockitoBean dev.reminishub.repository.UsuarioRepository usuarios;
    @MockitoBean dev.reminishub.repository.SesionesRevocadas revocadas;

    private void catalogoVacio() {
        when(catalogo.buscar(anyString(), anyString(), any(), any(), anyInt()))
                .thenReturn(new PaginaProyectos(List.of(), 0, 0, 0));
        when(catalogo.categoriasDisponibles()).thenReturn(List.of());
        when(catalogo.tecnologiasDisponibles()).thenReturn(List.of());
        when(catalogo.anios()).thenReturn(List.of());
    }

    @Test
    void catalogoSeRenderizaEnLosTresIdiomasSinClavesAusentes() throws Exception {
        catalogoVacio();
        for (String idioma : List.of("es", "en", "eo")) {
            mvc.perform(get("/proyectos").param("lang", idioma))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("lang=\"" + idioma + "\"")))
                    .andExpect(content().string(not(containsString("??"))));
        }
        mvc.perform(get("/proyectos").param("lang", "eo"))
                .andExpect(content().string(containsString("Esplori projektojn")))
                .andExpect(content().string(containsString("Ankoraŭ neniuj kongruoj")))
                .andExpect(content().string(containsString("name=\"lang\" value=\"eo\"")));
    }

    @Test
    void selectorConservaFiltrosYExponeEstadoAccesible() throws Exception {
        catalogoVacio();
        mvc.perform(get("/proyectos?q=prueba&categoria=WEB&lang=eo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-label=\"EO — Esperanto\"")))
                .andExpect(content().string(containsString("hreflang=\"es\"")))
                .andExpect(content().string(containsString(
                        "href=\"/proyectos?q=prueba&amp;categoria=WEB&amp;lang=es\"")))
                .andExpect(content().string(containsString("aria-current=\"true\"")));
    }
}
