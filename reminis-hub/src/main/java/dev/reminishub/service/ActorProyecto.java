package dev.reminishub.service;

import dev.reminishub.model.entity.Usuario;
import dev.reminishub.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Obtiene el actor del contexto autenticado y lo transmite al trigger en la conexión JPA actual. */
@Service
public class ActorProyecto {
    private final UsuarioRepository usuarios;
    private final EntityManager entidad;

    public ActorProyecto(UsuarioRepository usuarios, EntityManager entidad) {
        this.usuarios = usuarios;
        this.entidad = entidad;
    }

    /**
     * Exige una transacción existente: una transacción independiente perdería el actor
     * antes de escribir. No acepta IDs ni roles proporcionados por el formulario.
     * @return cuenta activa correspondiente al JWT autenticado por Spring Security
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Usuario establecer() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || !(autenticacion.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Actor autenticado requerido");
        }
        var usuario = usuarios.findByEmail(jwt.getSubject())
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .filter(u -> u.getRol().name().equals(jwt.getClaimAsString("rol")))
                .orElseThrow(() -> new AccessDeniedException("Cuenta no disponible"));
        entidad.createNativeQuery("select set_config('app.actor_user_id', :actor, true)")
                .setParameter("actor", usuario.getId().toString()).getSingleResult();
        return usuario;
    }
}
