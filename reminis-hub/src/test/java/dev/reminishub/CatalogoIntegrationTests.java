package dev.reminishub;

import dev.reminishub.service.CatalogoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** PostgreSQL real; los datos de cada prueba se revierten, sin depender de las semillas. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "reminishub.jwt.secret=" + SecretosDePrueba.JWT)
@EnabledIfEnvironmentVariable(named = "CATALOGO_DB_TESTS", matches = "true")
class CatalogoIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired CatalogoService catalogo;
    String marca;
    long usuario;
    long categoria;
    long tecnologia;
    List<Long> publicados;
    List<Long> privados;

    @BeforeEach
    void datosAislados() {
        marca = "Prueba" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        usuario = jdbc.queryForObject("INSERT INTO usuario(email,nombre,rol) VALUES (?,?,'ADMIN') RETURNING id",
                Long.class, marca.toLowerCase() + "@example.invalid", "Usuario ficticio de prueba");
        jdbc.queryForObject("select set_config('app.actor_user_id', ?, true)", String.class, Long.toString(usuario));
        categoria = jdbc.queryForObject("INSERT INTO categoria(codigo) VALUES (?) RETURNING id",
                Long.class, marca.toUpperCase());
        tecnologia = jdbc.queryForObject("INSERT INTO tecnologia(nombre) VALUES (?) RETURNING id",
                Long.class, marca);
        publicados = new ArrayList<>();
        privados = new ArrayList<>();
        for (int i = 0; i < 8; i++) publicados.add(insertar("PUBLICADO", i, 2026));
        for (String estado : List.of("PENDIENTE_REVISION", "RECHAZADO", "RETIRADO"))
            privados.add(insertar(estado, privados.size() + 8, 2026));
        jdbc.update("INSERT INTO proyecto_tecnologia VALUES (?,?)", publicados.getFirst(), tecnologia);
        jdbc.update("""
            INSERT INTO proyecto_autor(proyecto_id, nombre_publico, orden, medio_autorizacion,
                                       autorizado_en, version_autorizacion, referencia_evidencia)
            VALUES (?, 'Nombre público ficticio', 1, 'AUTORIZACION_DOCUMENTADA', now(), 'TEST', 'EVIDENCIA_PRIVADA_TEST')
            """, publicados.getFirst());
    }

    private long insertar(String estado, int numero, int anio) {
        long id = jdbc.queryForObject("""
            INSERT INTO proyecto(nombre,resumen,descripcion,url_repositorio,anio,semestre,categoria_id,
                estado,motivo_decision,cargado_por,terminos_aceptados_en,terminos_version,es_ficticio)
            VALUES (?, ?, ?, ?, ?, 1, ?, 'PENDIENTE_REVISION', NULL, ?, now(), 'TEST', false) RETURNING id
            """, Long.class, marca + " proyecto " + numero,
                "Resumen ficticio para verificar búsqueda y visibilidad de proyectos.",
                numero == 0 ? "<script>alert('dato ficticio')</script>" : "Descripción ficticia suficientemente larga para el contrato SQL.",
                "https://github.com/pruebas-reminishub/" + marca + numero,
                anio, categoria, usuario);
        if (!estado.equals("PENDIENTE_REVISION")) {
            jdbc.update("UPDATE proyecto SET estado=?,motivo_decision=? WHERE id=?", estado,
                    estado.equals("RECHAZADO") ? "Motivo privado" : null, id);
        }
        return id;
    }

    @Test
    void paginaSoloPublicadosConOrdenEstableYSinRepetidos() {
        var primera = catalogo.buscar(marca, null, null, null, 0);
        var segunda = catalogo.buscar(marca, null, null, null, 1);
        assertEquals(8, primera.total());
        assertEquals(2, primera.paginas());
        assertEquals(6, primera.contenido().size());
        assertEquals(2, segunda.contenido().size());
        assertEquals(publicados.getLast(), primera.contenido().getFirst().id());
        assertTrue(primera.contenido().stream().noneMatch(a -> segunda.contenido().stream()
                .anyMatch(b -> a.id().equals(b.id()))));
    }

    @Test
    void filtrosSeCombinanYLosComodinesSonLiterales() {
        assertEquals(1, catalogo.buscar(marca.toUpperCase(), marca.toUpperCase(), 2026, tecnologia, 0).total());
        assertEquals(0, catalogo.buscar(marca, marca.toUpperCase(), 2025, tecnologia, 0).total());
        assertEquals(0, catalogo.buscar(marca + "%", null, null, null, 0).total());
        assertEquals(0, catalogo.buscar(marca + "_", null, null, null, 0).total());
    }

    @Test
    void apiYDetalleNoRevelanPropuestasPrivadas() throws Exception {
        for (long id : privados) {
            mvc.perform(get("/api/v1/proyectos/{id}", id)).andExpect(status().isNotFound());
            mvc.perform(get("/proyectos/{id}", id)).andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/v1/proyectos").param("q", marca))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(8))
                .andExpect(jsonPath("$.contenido", hasSize(6)));
    }

    @Test
    void detalleExponeSoloDatosPublicosYEscapeHtml() throws Exception {
        long id = publicados.getFirst();
        mvc.perform(get("/api/v1/proyectos/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.autores[0]").value("Nombre público ficticio"))
                .andExpect(content().string(not(containsString("EVIDENCIA_PRIVADA_TEST"))))
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(content().string(not(containsString("usuarioId"))));
        mvc.perform(get("/proyectos/{id}", id)).andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert"))));
        // Los ejemplos versionados de V5 conservan su marca; no se falsea es_ficticio para nuevos registros.
        Long ejemplo = jdbc.queryForObject("SELECT min(id) FROM proyecto WHERE es_ficticio AND estado='PUBLICADO'", Long.class);
        assertNotNull(ejemplo);
        mvc.perform(get("/proyectos/{id}", ejemplo)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Datos ficticios")));
    }

    @Test
    void vistasYFragmentoFuncionanEnAmbosIdiomas() throws Exception {
        mvc.perform(get("/proyectos").param("q", marca)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Lo que otros crearon.")))
                .andExpect(content().string(containsString("jquery-3.7.1.min.js")));
        var ingles = mvc.perform(get("/proyectos").param("q", marca).param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Your next great idea.")))
                .andReturn().getResponse().getCookie("reminishub_lang");
        assertNotNull(ingles);
        mvc.perform(get("/catalogo/resultados").param("q", marca).cookie(ingles))
                .andExpect(status().isOk()).andExpect(content().string(containsString("projects found")))
                .andExpect(content().string(not(containsString("site-header"))));
        var esperanto = mvc.perform(get("/proyectos").param("q", marca).param("lang", "eo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"eo\"")))
                .andExpect(content().string(containsString("Via sekva granda ideo.")))
                .andReturn().getResponse().getCookie("reminishub_lang");
        assertNotNull(esperanto);
        mvc.perform(get("/catalogo/resultados").param("q", marca).cookie(esperanto))
                .andExpect(status().isOk()).andExpect(content().string(containsString("projektoj trovitaj")));
    }

    @Test
    void escriturasSiguenProtegidasPorCsrf() throws Exception {
        mvc.perform(post("/api/v1/proyectos").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void documentacionPublicaDescribeElCatalogo() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/proyectos'].get").exists());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void paginaFueraDeRangoPermiteVolverConFiltros() throws Exception {
        for (String idioma : List.of("es", "en", "eo")) {
            String fueraDeRango = switch (idioma) {
                case "es" -> "Esta página no está disponible";
                case "en" -> "This page is unavailable";
                default -> "Ĉi tiu paĝo ne estas disponebla";
            };
            mvc.perform(get("/proyectos").param("q", marca).param("anio", "2026")
                            .param("pagina", "10").param("lang", idioma))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(fueraDeRango)))
                    .andExpect(content().string(matchesPattern("(?s).*href=\"/proyectos\\?q="
                            + marca + "[^\"]*&amp;anio=2026[^\"]*&amp;pagina=0&amp;lang="
                            + idioma + "\".*")))
                    .andExpect(content().string(not(containsString("class=\"pagination-bar\""))));
        }
    }

    @Test
    void contadorSingularSeTraduce() throws Exception {
        for (String idioma : List.of("es", "en", "eo")) {
            String singular = switch (idioma) {
                case "es" -> "1 proyecto encontrado";
                case "en" -> "1 project found";
                default -> "1 projekto trovita";
            };
            mvc.perform(get("/catalogo/resultados").param("q", marca)
                            .param("tecnologia", Long.toString(tecnologia)).param("lang", idioma))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(singular)));
        }
    }

    // Tu turno: agrega una prueba GET /catalogo/resultados con q=marca + "ausente"
    // y lang=en. Comprueba 200, "0 projects found" y "No matches yet".
    @Test
    void validarEstadoVacioEn() throws Exception {
        mvc.perform(get("/catalogo/resultados").param("q", marca + "ausente")
                .param("lang", "en")).andExpect(status().isOk())
                .andExpect(content().string(containsString("0 projects found")))
                .andExpect(content().string(containsString("No matches yet")));
    }

    // Ejercicio del estudiante: una página negativa debe rechazarse antes de consultar.
    @Test
    void invalidarPaginasValorNegativo() throws Exception {
        mvc.perform(get("/api/v1/proyectos").param("pagina", "-1"))
                .andExpect(status().isBadRequest());
    }
}
