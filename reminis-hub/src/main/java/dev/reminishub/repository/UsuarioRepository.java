package dev.reminishub.repository;

import dev.reminishub.model.entity.Usuario;
import dev.reminishub.repository.UsuarioRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByEmail(String email);
    Optional<Usuario> findByEmail(String email);
}
