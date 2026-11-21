package dev.reminishub;

import dev.reminishub.config.JwtConfig;
import dev.reminishub.config.SecurityConfig;
import dev.reminishub.config.WebConfig;
import dev.reminishub.controller.api.ReporteApiController;
import dev.reminishub.controller.web.ReporteWebController;
import dev.reminishub.dto.EntradaBitacora;
import dev.reminishub.dto.FilaReporte;
import dev.reminishub.dto.PaginaBitacora;
import dev.reminishub.service.ReporteService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Renderizado real ES/EN, contrato CSV y permisos HTTP; el servicio se verifica por separado. */
@WebMvcTest(controllers={ReporteApiController.class,ReporteWebController.class},properties={
        "reminishub.jwt.secret="+SecretosDePrueba.JWT,"spring.messages.basename=messages,reporte"})
@Import({SecurityConfig.class,JwtConfig.class,WebConfig.class})
class ReporteWebTests {
    @Autowired MockMvc mvc;
    @MockitoBean ReporteService reportes;
    @MockitoBean dev.reminishub.repository.UsuarioRepository usuarios;
    @MockitoBean dev.reminishub.repository.SesionesRevocadas revocadas;

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject("admin@example.invalid").claim("rol","ADMIN"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test void apiAnonimaDevuelve401YColaborador403() throws Exception {
        mvc.perform(get("https://localhost/api/v1/admin/reporte").secure(true)).andExpect(status().isUnauthorized());
        mvc.perform(get("https://localhost/api/v1/admin/reporte").secure(true)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_COLABORADOR"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reportes);
    }

    @Test void reporteRenderizaAmbosIdiomasYFiltrosDeExportacion() throws Exception {
        when(reportes.codigosDeCategoria()).thenReturn(List.of("WEB","MOVIL"));
        when(reportes.consultar(2026,"WEB")).thenReturn(List.of(new FilaReporte(2026,"WEB","PUBLICADO",3,1)));
        for (String idioma : List.of("es","en","eo")) {
            String estado = switch (idioma) {
                case "es" -> "Publicado";
                case "en" -> "Published";
                default -> "Publikigita";
            };
            mvc.perform(get("https://localhost/admin/reporte").secure(true).with(admin())
                            .param("lang",idioma).param("anio","2026").param("categoria","WEB"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(estado)))
                    .andExpect(content().string(matchesPattern("(?s).*<option(?=[^>]*value=\"WEB\")(?=[^>]*selected)[^>]*>[^<]+</option>.*")))
                    .andExpect(content().string(containsString("/admin/reporte.csv?anio=2026&amp;categoria=WEB")))
                    .andExpect(content().string(not(containsString("??reporte."))));
        }
    }

    @Test void vacioYBitacoraEscapanContenidoDeUsuario() throws Exception {
        when(reportes.consultar(null,null)).thenReturn(List.of());
        mvc.perform(get("https://localhost/admin/reporte").secure(true).with(admin()).param("lang","en"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("No projects match")));
        var fila = new EntradaBitacora(1,1,"CAMBIO_ESTADO","PENDIENTE_REVISION","RECHAZADO",1L,
                "<script>alert(1)</script>",Instant.parse("2026-11-20T10:00:00Z"));
        when(reportes.bitacora(0)).thenReturn(new PaginaBitacora(List.of(fila),0,true));
        mvc.perform(get("https://localhost/admin/bitacora").secure(true).with(admin()).param("lang","en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert"))))
                .andExpect(content().string(containsString("/admin/bitacora?pagina=1")))
                .andExpect(content().string(containsString("Rejected")));
    }

    @Test void vacioYBitacoraEnEsperanto() throws Exception {
        when(reportes.consultar(null,null)).thenReturn(List.of());
        mvc.perform(get("https://localhost/admin/reporte").secure(true).with(admin()).param("lang","eo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ne estas projektoj kun ĉi tiuj filtriloj")));
        var fila = new EntradaBitacora(1,1,"CAMBIO_ESTADO","PENDIENTE_REVISION","RECHAZADO",1L,
                "Motivo de prueba",Instant.parse("2026-11-20T10:00:00Z"));
        when(reportes.bitacora(0)).thenReturn(new PaginaBitacora(List.of(fila),0,true));
        mvc.perform(get("https://localhost/admin/bitacora").secure(true).with(admin()).param("lang","eo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"eo\"")))
                .andExpect(content().string(containsString("Statŝanĝo")))
                .andExpect(content().string(containsString("Rifuzita")))
                .andExpect(content().string(containsString("/admin/bitacora?pagina=1&amp;lang=eo")));
    }

    @Test void descargaUsaTipoYNombreFijadosPorServidor() throws Exception {
        when(reportes.exportar(eq(2026),eq("WEB"),any())).thenReturn("\"Year\"\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mvc.perform(get("https://localhost/api/v1/admin/reporte.csv").secure(true).with(admin())
                        .param("anio","2026").param("categoria","WEB"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition","attachment; filename=\"reminishub-reporte.csv\""));
    }
}
