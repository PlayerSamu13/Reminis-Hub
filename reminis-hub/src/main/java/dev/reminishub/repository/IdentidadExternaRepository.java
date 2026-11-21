package dev.reminishub.repository;

import dev.reminishub.model.entity.IdentidadExterna;
import dev.reminishub.model.enums.ProveedorExterno;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentidadExternaRepository extends JpaRepository<IdentidadExterna, Long> {
    Optional<IdentidadExterna> findByProveedorAndSubject(ProveedorExterno proveedor, String subject);
}
