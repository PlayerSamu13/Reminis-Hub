package dev.reminishub;

import dev.reminishub.config.JwtConfig;
import dev.reminishub.config.SecurityConfig;
import dev.reminishub.config.WebConfig;
import dev.reminishub.controller.web.PropuestasWebController;
import dev.reminishub.dto.*;
import dev.reminishub.service.CatalogoService;
import dev.reminishub.service.PropuestasService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Renderizado real Thymeleaf, idiomas y CSRF; la persistencia se prueba por separado. */
@WebMvcTest(controllers = PropuestasWebController.class, properties = {
        "reminishub.jwt.secret=" + SecretosDePrueba.JWT,
        "spring.messages.basename=messages,propuestas"})
@Import({SecurityConfig.class, JwtConfig.class, WebConfig.class})
@WithMockUser(roles = "COLABORADOR")
class PropuestasWebTests {
    @Autowired MockMvc mvc;
    @MockitoBean PropuestasService propuestas;
    @MockitoBean CatalogoService catalogo;
    @MockitoBean dev.reminishub.repository.UsuarioRepository usuarios;
    @MockitoBean dev.reminishub.repository.SesionesRevocadas revocadas;

    @Test void formularioYListaSeRenderizanEnAmbosIdiomas() throws Exception {
        when(catalogo.categoriasDisponibles()).thenReturn(List.of());
        when(catalogo.tecnologiasDisponibles()).thenReturn(List.of());
        when(propuestas.listar(0,false)).thenReturn(List.of());
        for (String idioma : List.of("es","en","eo")) {
            String nueva = switch (idioma) {
                case "es" -> "Presentar proyecto";
                case "en" -> "Submit project";
                default -> "Prezenti projekton";
            };
            String vacia = switch (idioma) {
                case "es" -> "No hay propuestas";
                case "en" -> "There are no proposals";
                default -> "Ne estas proponoj";
            };
            mvc.perform(get("https://localhost/propuestas/nueva").secure(true).param("lang",idioma))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(nueva)))
                    .andExpect(content().string(containsString("name=\"_csrf\"")))
                    .andExpect(content().string(not(containsString("??propuestas."))));
            mvc.perform(get("https://localhost/mis-propuestas").secure(true).param("lang",idioma))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(vacia)));
        }
    }

    @Test void detalleEscapaContenidoYEstadoEsTraducido() throws Exception {
        when(propuestas.detalle(1)).thenReturn(new PropuestaDetalle(
                new PropuestaResumen(1L,"<script>peligro</script>","RECHAZADO",2L,"Completa los datos"),
                "Descripcion de prueba", "Resumen de prueba", "https://github.com/prueba/repositorio",
                2026,2,1L,List.of(),List.of(),true));
        for (String idioma : List.of("es","en","eo")) {
            String estado = switch (idioma) {
                case "es" -> "Rechazado";
                case "en" -> "Rejected";
                default -> "Rifuzita";
            };
            mvc.perform(get("https://localhost/propuestas/1").secure(true).param("lang",idioma))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("&lt;script&gt;")))
                    .andExpect(content().string(not(containsString("<script>peligro"))))
                    .andExpect(content().string(containsString(estado)));
        }
    }

    @Test void datosInvalidosConservanFormularioSinEscribir() throws Exception {
        when(catalogo.categoriasDisponibles()).thenReturn(List.of());
        when(catalogo.tecnologiasDisponibles()).thenReturn(List.of());
        mvc.perform(post("https://localhost/propuestas/nueva").secure(true).with(csrf()).param("nombre","X"))
                .andExpect(status().isBadRequest()).andExpect(content().string(containsString("errores-propuesta")));
        verifyNoInteractions(propuestas);
    }

    @Test void csrfEsObligatorioParaCrear() throws Exception {
        mvc.perform(post("https://localhost/propuestas/nueva").secure(true)).andExpect(status().isForbidden());
        verifyNoInteractions(propuestas);
    }

    @Test void conflictoNoExponeMensajeInterno() throws Exception {
        doThrow(new org.springframework.dao.OptimisticLockingFailureException("detalle privado"))
                .when(propuestas).retirar(1,2);
        mvc.perform(post("https://localhost/propuestas/1/retirar").secure(true).with(csrf()).param("version","2"))
                .andExpect(status().isConflict())
                .andExpect(content().string(not(containsString("detalle privado"))))
                .andExpect(content().string(containsString("Recarga el detalle")));
    }
}
