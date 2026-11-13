package dev.reminishub.repository;

import dev.reminishub.model.entity.Tecnologia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TecnologiaRepository extends JpaRepository<Tecnologia, Long> {
    List<Tecnologia> findAllByOrderByNombreAsc();
}
