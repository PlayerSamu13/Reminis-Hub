package dev.reminishub.service;

import dev.reminishub.dto.*;
import dev.reminishub.model.enums.RolUsuario;
import dev.reminishub.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Consulta administrativa JDBC: agregados contrastables con SQL y bitácora de solo lectura. */
@Service
@Transactional(readOnly = true)
public class ReporteService {
    private final JdbcTemplate jdbc;
    private final UsuarioRepository usuarios;
    private final MessageSource mensajes;

    public ReporteService(JdbcTemplate jdbc, UsuarioRepository usuarios, MessageSource mensajes) {
        this.jdbc = jdbc;
        this.usuarios = usuarios;
        this.mensajes = mensajes;
    }

    /**
     * Agrupa proyectos de todos los estados y distingue los ejemplos ficticios.
     * @param anio filtro opcional entre 2000 y 2100
     * @param categoria código opcional del catálogo, nunca un fragmento SQL
     * @return filas ordenadas por año descendente, categoría y estado
     */
    public List<FilaReporte> consultar(Integer anio, String categoria) {
        exigirAdministrador();
        String codigo = categoria == null ? "" : categoria.strip();
        if ((anio != null && (anio < 2000 || anio > 2100))
                || (!codigo.isEmpty() && !codigo.matches("[A-Z][A-Z0-9_]{0,29}")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        StringBuilder sql = new StringBuilder("""
                SELECT p.anio, c.codigo, p.estado, count(*) AS total,
                       count(*) FILTER (WHERE p.es_ficticio) AS ficticios
                FROM proyecto p JOIN categoria c ON c.id=p.categoria_id WHERE 1=1
                """);
        List<Object> parametros = new ArrayList<>();
        if (anio != null) { sql.append(" AND p.anio=?"); parametros.add(anio); }
        if (!codigo.isEmpty()) { sql.append(" AND c.codigo=?"); parametros.add(codigo); }
        sql.append(" GROUP BY p.anio,c.codigo,p.estado ORDER BY p.anio DESC,c.codigo,p.estado");
        return jdbc.query(sql.toString(), (rs, n) -> new FilaReporte(rs.getInt("anio"),
                rs.getString("codigo"), rs.getString("estado"), rs.getLong("total"),
                rs.getLong("ficticios")), parametros.toArray());
    }

    /** Incluye categorías históricas para poder filtrar cualquier proyecto del reporte. */
    public List<String> codigosDeCategoria() {
        exigirAdministrador();
        return jdbc.queryForList("SELECT codigo FROM categoria ORDER BY codigo", String.class);
    }

    /** Descarga web con firma UTF-8 e indicación del separador para Excel de escritorio. */
    public byte[] exportarParaExcel(Integer anio, String categoria, Locale idioma) {
        return exportar(anio, categoria, idioma, true);
    }

    /** Exporta los mismos agregados en CSV UTF-8; el nombre de descarga lo fija el servidor. */
    public byte[] exportar(Integer anio, String categoria, Locale idioma) {
        return exportar(anio, categoria, idioma, false);
    }

    private byte[] exportar(Integer anio, String categoria, Locale idioma, boolean excel) {
        List<FilaReporte> filas = consultar(anio, categoria);
        StringBuilder csv = new StringBuilder(excel ? "\uFEFFsep=,\r\n" : "");
        csv.append(linea(List.of(texto("reporte.anio",idioma), texto("reporte.categoria",idioma),
                texto("reporte.estado",idioma), texto("reporte.total",idioma), texto("reporte.ficticios",idioma))));
        for (FilaReporte fila : filas) {
            csv.append(linea(List.of(Integer.toString(fila.anio()), fila.categoria(), fila.estado(),
                    Long.toString(fila.total()), Long.toString(fila.ficticios()))));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String texto(String clave, Locale idioma) { return mensajes.getMessage(clave,null,idioma); }

    /**
     * Escapa CSV con comillas y CRLF. Prefija apóstrofo ante fórmulas o controles iniciales.
     * Es defensa para importadores de hojas de cálculo, no garantía universal: importar como texto.
     */
    static String linea(List<String> valores) {
        return valores.stream().map(ReporteService::celda).collect(java.util.stream.Collectors.joining(",")) + "\r\n";
    }

    static String celda(String valor) {
        String seguro = valor == null ? "" : valor;
        String inicio = seguro.stripLeading();
        if ((!inicio.isEmpty() && "=+-@".indexOf(inicio.charAt(0)) >= 0)
                || (!seguro.isEmpty() && Character.isISOControl(seguro.charAt(0)))) seguro = "'" + seguro;
        return "\"" + seguro.replace("\"", "\"\"") + "\"";
    }

    /** Consulta veinte hechos, con desempate por ID y una fila adicional para detectar página siguiente. */
    public PaginaBitacora bitacora(int pagina) {
        exigirAdministrador();
        if (pagina < 0 || pagina > 10000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        List<EntradaBitacora> filas = jdbc.query("""
                SELECT id,proyecto_id,operacion,estado_anterior,estado_nuevo,
                       usuario_aplicacion_id,motivo,registrado_en
                FROM bitacora_proyecto ORDER BY registrado_en DESC,id DESC LIMIT 21 OFFSET ?
                """, (rs,n) -> new EntradaBitacora(rs.getLong("id"),rs.getLong("proyecto_id"),
                rs.getString("operacion"),rs.getString("estado_anterior"),rs.getString("estado_nuevo"),
                rs.getObject("usuario_aplicacion_id",Long.class),rs.getString("motivo"),
                rs.getTimestamp("registrado_en").toInstant()), pagina * 20);
        return new PaginaBitacora(List.copyOf(filas.subList(0,Math.min(20,filas.size()))),pagina,filas.size()>20);
    }

    private void exigirAdministrador() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || !(autenticacion.getPrincipal() instanceof Jwt jwt)
                || !"ADMIN".equals(jwt.getClaimAsString("rol"))
                || usuarios.findByEmail(jwt.getSubject()).filter(u -> Boolean.TRUE.equals(u.getActivo()))
                    .filter(u -> u.getRol() == RolUsuario.ADMIN).isEmpty())
            throw new AccessDeniedException("Administración requerida");
    }
}
