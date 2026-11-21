package dev.reminishub;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paridad de los bundles ES/EN/EO sin arrancar Spring ni PostgreSQL.
 */
class IdiomaBundlesTests {
    private static final List<String> BASES = List.of("messages", "propuestas", "reporte");
    private static final List<String> SUFIJOS = List.of("", "_en", "_eo");

    private static Properties cargar(String base, String sufijo) throws Exception {
        String ruta = "/" + base + sufijo + ".properties";
        try (InputStream entrada = IdiomaBundlesTests.class.getResourceAsStream(ruta)) {
            assertNotNull(entrada, "Falta el bundle " + ruta);
            var propiedades = new Properties();
            propiedades.load(new InputStreamReader(entrada, StandardCharsets.UTF_8));
            return propiedades;
        }
    }

    @Test
    void losTresIdiomasTienenLasMismasClaves() throws Exception {
        for (String base : BASES) {
            var espanol = new TreeSet<>(cargar(base, "").stringPropertyNames());
            for (String sufijo : SUFIJOS.subList(1, SUFIJOS.size())) {
                var otro = new TreeSet<>(cargar(base, sufijo).stringPropertyNames());
                assertEquals(espanol, otro, "Claves distintas en " + base + sufijo);
            }
        }
    }

    @Test
    void ningunMensajeEstaVacio() throws Exception {
        for (String base : BASES) {
            for (String sufijo : SUFIJOS) {
                var propiedades = cargar(base, sufijo);
                for (String clave : propiedades.stringPropertyNames()) {
                    assertFalse(propiedades.getProperty(clave).isBlank(),
                            "Valor vacio en " + base + sufijo + ": " + clave);
                }
            }
        }
    }

    @Test
    void losBundlesEsperantoSeLeenComoUtf8() throws Exception {
        assertEquals("Ĉefa navigado", cargar("messages", "_eo").getProperty("nav.main"));
        assertEquals("Rifuzita", cargar("propuestas", "_eo").getProperty("propuestas.estado.RECHAZADO"));
        assertEquals("Statŝanĝo", cargar("reporte", "_eo").getProperty("bitacora.operacion.CAMBIO_ESTADO"));
        assertTrue(cargar("messages", "_eo").getProperty("categoria.WEB").contains("Reta"),
                "El bundle EO no debe contener texto en espanol");
    }

    // Tu turno: agrega una prueba que compare los marcadores {0}/{1} de cada clave
    // entre messages.properties, messages_en.properties y messages_eo.properties.
    // Pista: usa Pattern.compile("\\{\\d+\\}") y compara los conjuntos por clave.
    private static final Pattern MARCADOR = Pattern.compile("\\{\\d+\\}");

    private static Set<String> marcadores(String texto) {
        var encontrados = new TreeSet<String>();
        var coincidencia = MARCADOR.matcher(texto);
        while (coincidencia.find()) {
            encontrados.add(coincidencia.group());
        }
        return encontrados;
    }

    @Test
    void compararMarcadorIdiomas() throws Exception {
        var espanol = cargar("messages", "");
        for (String sufijo : List.of("_en", "_eo")) {
            var otroIdioma = cargar("messages", sufijo);
            for (String clave : espanol.stringPropertyNames()) {
                assertEquals(marcadores(espanol.getProperty(clave)),
                        marcadores(otroIdioma.getProperty(clave)),
                        "Marcadores distintos en messages" + sufijo + ": " + clave);
            }
        }
    }
}
