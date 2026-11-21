package dev.reminishub.service;

import dev.reminishub.model.entity.Usuario;
import dev.reminishub.model.enums.RolUsuario;
import dev.reminishub.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReporteServiceTests {
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    final ReporteService reportes = new ReporteService(jdbc,usuarios,mock(MessageSource.class));
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }

    void identidad(String rol) {
        var jwt = Jwt.withTokenValue("ficticio").header("alg","HS256").subject("admin@example.invalid")
                .claim("rol",rol).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,List.of()));
    }

    @Test void anonimoYColaboradorNoConsultanDatos() {
        assertThrows(AccessDeniedException.class, () -> reportes.consultar(null,null));
        identidad("COLABORADOR");
        assertThrows(AccessDeniedException.class, () -> reportes.bitacora(0));
        verifyNoInteractions(jdbc);
    }

    @Test void adminDesactivadoODemoradoNoConsultaDatos() {
        identidad("ADMIN");
        var cuenta = new Usuario();
        cuenta.setRol(RolUsuario.ADMIN);
        cuenta.setActivo(false);
        when(usuarios.findByEmail("admin@example.invalid")).thenReturn(Optional.of(cuenta));
        assertThrows(AccessDeniedException.class, () -> reportes.consultar(null,null));
        cuenta.setActivo(true);
        cuenta.setRol(RolUsuario.COLABORADOR);
        assertThrows(AccessDeniedException.class, () -> reportes.bitacora(0));
        verifyNoInteractions(jdbc);
    }

    @Test void filtrosInvalidosNoLleganAJdbc() {
        identidad("ADMIN");
        var cuenta = new Usuario(); cuenta.setRol(RolUsuario.ADMIN);
        when(usuarios.findByEmail("admin@example.invalid")).thenReturn(Optional.of(cuenta));
        for (Integer anio : List.of(1999,2101))
            assertEquals(400,assertThrows(ResponseStatusException.class,() -> reportes.consultar(anio,null)).getStatusCode().value());
        assertThrows(ResponseStatusException.class, () -> reportes.consultar(null,"WEB' OR 1=1"));
        assertThrows(ResponseStatusException.class, () -> reportes.bitacora(-1));
        assertThrows(ResponseStatusException.class, () -> reportes.bitacora(10001));
        verifyNoInteractions(jdbc);
    }

    @Test void descargaExcelDeclaraUtf8YSeparadorAntesDeLasCincoColumnas() {
        var mensajes = new org.springframework.context.support.StaticMessageSource();
        var idioma = java.util.Locale.forLanguageTag("es");
        var titulos = List.of("Año", "Código de categoría", "Estado", "Total", "Datos ficticios");
        var claves = List.of("anio", "categoria", "estado", "total", "ficticios");
        for (int i = 0; i < claves.size(); i++)
            mensajes.addMessage("reporte." + claves.get(i), idioma, titulos.get(i));
        var servicio = spy(new ReporteService(jdbc, usuarios, mensajes));
        doReturn(List.of(new dev.reminishub.dto.FilaReporte(2026,"WEB","PUBLICADO",2,1)))
                .when(servicio).consultar(2026,"WEB");
        byte[] bytes = servicio.exportarParaExcel(2026,"WEB",idioma);
        assertArrayEquals(new byte[]{(byte)0xef,(byte)0xbb,(byte)0xbf}, java.util.Arrays.copyOf(bytes,3));
        assertEquals("\uFEFFsep=,\r\n" + ReporteService.linea(titulos)
                + ReporteService.linea(List.of("2026","WEB","PUBLICADO","2","1")),
                new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test void csvEscapaComillasComasYSaltos() {
        assertEquals("\"a,b\",\"c\"\"d\",\"dos\nlineas\"\r\n", ReporteService.linea(List.of("a,b","c\"d","dos\nlineas")));
    }

    @Test void csvNeutralizaFormulaYControlInicial() {
        for (String dato : List.of("=1+1","+cmd","-1","@SUM(A1)","  =1","\t=1","\r=1","\n=1"))
            assertTrue(ReporteService.celda(dato).startsWith("\"'"),dato);
        assertEquals("\"normal\"",ReporteService.celda("normal"));
        assertEquals("\"\"",ReporteService.celda(null));
    }
}
