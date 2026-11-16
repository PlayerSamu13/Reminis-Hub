package dev.reminishub.service;

import dev.reminishub.dto.*;
import dev.reminishub.model.entity.Categoria;
import dev.reminishub.model.entity.Proyecto;
import dev.reminishub.model.entity.ProyectoAutor;
import dev.reminishub.model.entity.Tecnologia;
import dev.reminishub.model.enums.EstadoProyecto;
import dev.reminishub.model.enums.MedioAutorizacion;
import dev.reminishub.repository.CategoriaRepository;
import dev.reminishub.repository.ProyectoRepository;
import dev.reminishub.repository.TecnologiaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class CatalogoService {
    private final ProyectoRepository proyectos;
    private final CategoriaRepository categorias;
    private final TecnologiaRepository tecnologias;

    public CatalogoService(ProyectoRepository proyectos, CategoriaRepository categorias, TecnologiaRepository tecnologias) {
        this.proyectos = proyectos;
        this.categorias = categorias;
        this.tecnologias = tecnologias;
    }

    /** Busca únicamente publicados, con seis resultados y orden determinista por periodo e ID. */
    public PaginaProyectos buscar(String q, String categoria, Integer anio, Long tecnologia, int pagina) {
        if (q == null) q = "";
        if (q.length() > 120 || pagina < 0 || pagina > 10000
                || (anio != null && (anio < 2000 || anio > 2100))
                || (tecnologia != null && tecnologia <= 0)
                || (categoria != null && categoria.length() > 30)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        // LIKE trata % y _ como operadores: escaparlos mantiene la búsqueda literal.
        String texto = "%" + q.strip().toLowerCase(Locale.ROOT).replace("!", "!!")
                .replace("%", "!%").replace("_", "!_") + "%";
        String codigo = categoria == null || categoria.isBlank() ? null : categoria;
        var resultado = proyectos.buscar(EstadoProyecto.PUBLICADO, texto, codigo,
                anio == null ? null : anio.shortValue(), tecnologia,
                PageRequest.of(pagina, 6, Sort.by(Sort.Direction.DESC, "anio", "semestre", "id")));
        // La conversión ocurre dentro de la transacción: las vistas no cargan relaciones lazy.
        return new PaginaProyectos(resultado.getContent().stream().map(this::resumen).toList(),
                pagina, resultado.getTotalPages(), resultado.getTotalElements());
    }

    /** Devuelve 404 tanto para un ID inexistente como para una propuesta privada. */
    public ProyectoDetalle detalle(long id) {
        var p = proyectos.findByIdAndEstado(id, EstadoProyecto.PUBLICADO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var autores = p.getAutores().stream().filter(a -> autorVisible(a, p))
                .sorted(Comparator.comparing(ProyectoAutor::getOrden))
                .map(ProyectoAutor::getNombrePublico).toList();
        return new ProyectoDetalle(resumen(p), p.getDescripcion(), p.getAsignatura(),
                p.getUrlRepositorio(), autores);
    }

    public List<Short> anios() { return proyectos.aniosPublicados(EstadoProyecto.PUBLICADO); }
    /**
     * Obtiene las categorías disponibles para filtrar el catálogo.
     *
     * @return categorías activas ordenadas por orden y código
     */
    public List<Categoria> categoriasDisponibles() {
        return categorias.findByActivaTrueOrderByOrdenAscCodigoAsc();
    }

    /**
     * Obtiene las tecnologías disponibles para filtrar el catálogo.
     *
     * @return tecnologías ordenadas por nombre
     */
    public List<Tecnologia> tecnologiasDisponibles() {
        return tecnologias.findAllByOrderByNombreAsc();
    }

    private ProyectoResumen resumen(Proyecto p) {
        return new ProyectoResumen(p.getId(), p.getNombre(), p.getResumen(), p.getCategoria().getCodigo(),
                p.getAnio(), p.getSemestre(), Boolean.TRUE.equals(p.getEsFicticio()),
                p.getTecnologias().stream().map(Tecnologia::getNombre).sorted().toList());
    }

    // Defensa adicional ante registros incompletos. La revisión administrativa del
    // consentimiento real sigue siendo necesaria en el futuro flujo de publicación.
    private boolean autorVisible(ProyectoAutor a, Proyecto p) {
        if (a.getMedioAutorizacion() == MedioAutorizacion.DATO_FICTICIO)
            return Boolean.TRUE.equals(p.getEsFicticio());
        if (a.getAutorizadoEn() == null || a.getVersionAutorizacion() == null
                || a.getVersionAutorizacion().isBlank()) return false;
        return switch (a.getMedioAutorizacion()) {
            case CUENTA_PROPIA -> a.getUsuario() != null;
            case AUTORIZACION_DOCUMENTADA -> a.getReferenciaEvidencia() != null
                    && !a.getReferenciaEvidencia().isBlank();
            default -> false;
        };
    }
}
