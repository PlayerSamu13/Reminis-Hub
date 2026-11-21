package dev.reminishub;

import dev.reminishub.service.ReporteService;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

/** Contraste de agregados con SQL directo en PostgreSQL; la cuenta administrativa ficticia se revierte. */
@SpringBootTest(properties="reminishub.jwt.secret="+SecretosDePrueba.JWT)
@Transactional
@EnabledIfEnvironmentVariable(named="CATALOGO_DB_TESTS",matches="true")
class ReporteIntegrationTests {
    @DynamicPropertySource static void baseAislada(DynamicPropertyRegistry propiedades) {
        String url = System.getenv("DB_URL");
        if (!"jdbc:postgresql://localhost:5432/reminishub_test".equals(url))
            throw new IllegalStateException("Solo se permite reminishub_test");
        propiedades.add("spring.datasource.url",() -> url);
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired ReporteService reportes;

    @BeforeEach void administradorTemporal() {
        String email=UUID.randomUUID()+"@example.invalid";
        jdbc.update("INSERT INTO usuario(email,nombre,rol) VALUES (?,?,'ADMIN')",email,"Prueba reporte");
        var jwt=Jwt.withTokenValue("prueba").header("alg","HS256").subject(email).claim("rol","ADMIN").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,List.of()));
    }
    @AfterEach void limpiarIdentidad() { SecurityContextHolder.clearContext(); }

    @Test void agregadosCoincidenConConteosDirectosYFiltros() {
        var filas=reportes.consultar(null,null);
        assertEquals(jdbc.queryForObject("SELECT count(*) FROM proyecto",Long.class).longValue(),
                filas.stream().mapToLong(f -> f.total()).sum());
        assertEquals(jdbc.queryForObject("SELECT count(*) FROM proyecto WHERE es_ficticio",Long.class).longValue(),
                filas.stream().mapToLong(f -> f.ficticios()).sum());
        for (var fila : filas) {
            long directo=jdbc.queryForObject("""
                    SELECT count(*) FROM proyecto WHERE anio=? AND estado=?
                    AND categoria_id=(SELECT id FROM categoria WHERE codigo=?)
                    """,Long.class,fila.anio(),fila.estado(),fila.categoria());
            assertEquals(directo,fila.total());
            assertTrue(fila.ficticios()<=fila.total());
            var filtro=reportes.consultar(fila.anio(),fila.categoria());
            assertTrue(filtro.stream().allMatch(f -> f.anio()==fila.anio() && f.categoria().equals(fila.categoria())));
        }
    }

    @Test void filtroSinCoincidenciasDevuelveCsvSoloConCabecera() {
        String categoria="PRUEBA"+UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase(Locale.ROOT);
        assertTrue(reportes.consultar(null,categoria).isEmpty());
        String csv=new String(reportes.exportar(null,categoria,Locale.ENGLISH),java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(1,csv.lines().count());
        assertTrue(csv.startsWith("\"Year\""));
    }
}
