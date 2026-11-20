package dev.reminishub.service;

import dev.reminishub.model.entity.Usuario;
import dev.reminishub.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Reglas de identidad del actor; la transacción/conexión se comprueba en la suite PostgreSQL. */
class ActorProyectoTests {
    final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    final EntityManager em = mock(EntityManager.class);
    final ActorProyecto actor = new ActorProyecto(usuarios, em);

    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }

    void autenticar(String rol) {
        var jwt = Jwt.withTokenValue("prueba").header("alg","HS256").subject("actor@example.invalid")
                .claim("rol",rol).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, java.util.List.of()));
    }

    @Test void anonimoNoEstableceContextoDeBase() {
        assertThrows(AccessDeniedException.class, actor::establecer);
        verifyNoInteractions(usuarios,em);
    }

    @Test void cuentaAusenteInactivaORolDistintoNoEstableceActor() {
        autenticar("COLABORADOR");
        when(usuarios.findByEmail("actor@example.invalid")).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, actor::establecer);
        var cuenta = new Usuario();
        cuenta.setActivo(false);
        when(usuarios.findByEmail("actor@example.invalid")).thenReturn(Optional.of(cuenta));
        assertThrows(AccessDeniedException.class, actor::establecer);
        cuenta.setActivo(true);
        autenticar("ADMIN");
        assertThrows(AccessDeniedException.class, actor::establecer);
        verifyNoInteractions(em);
    }

    @Test void idProvieneDeCuentaDelServidor() {
        autenticar("COLABORADOR");
        var cuenta = new Usuario();
        cuenta.setId(73L);
        when(usuarios.findByEmail("actor@example.invalid")).thenReturn(Optional.of(cuenta));
        Query consulta = mock(Query.class);
        when(em.createNativeQuery("select set_config('app.actor_user_id', :actor, true)")).thenReturn(consulta);
        when(consulta.setParameter("actor","73")).thenReturn(consulta);
        assertSame(cuenta, actor.establecer());
        verify(consulta).getSingleResult();
    }
}
