package dev.reminishub.repository;

import dev.reminishub.model.entity.Proyecto;
import dev.reminishub.model.enums.EstadoProyecto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {
    @EntityGraph(attributePaths = "categoria")
    @Query("""
        select p from Proyecto p
        where p.estado = :estado
          and (lower(p.nombre) like :texto escape '!' or lower(p.resumen) like :texto escape '!')
          and (:categoria is null or p.categoria.codigo = :categoria)
          and (:anio is null or p.anio = :anio)
          and (:tecnologia is null or exists
               (select t.id from p.tecnologias t where t.id = :tecnologia))
        """)
    Page<Proyecto> buscar(@Param("estado") EstadoProyecto estado, @Param("texto") String texto,
                          @Param("categoria") String categoria, @Param("anio") Short anio,
                          @Param("tecnologia") Long tecnologia, Pageable pagina);

    @EntityGraph(attributePaths = "categoria")
    Optional<Proyecto> findByIdAndEstado(Long id, EstadoProyecto estado);

    @Query("select distinct p.anio from Proyecto p where p.estado = :estado order by p.anio desc")
    List<Short> aniosPublicados(@Param("estado") EstadoProyecto estado);
}
