package dev.reminishub.repository;

import dev.reminishub.model.entity.Proyecto;
import dev.reminishub.model.enums.EstadoProyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;

/** Escritura y consultas privadas; el catálogo público conserva su repositorio y filtro PUBLICADO. */
public interface PropuestasRepository extends JpaRepository<Proyecto,Long> {
    List<Proyecto> findByCargadoPorIdOrderByIdDesc(Long usuarioId,Pageable pagina);
    List<Proyecto> findByEstadoOrderByIdDesc(EstadoProyecto estado,Pageable pagina);
}
