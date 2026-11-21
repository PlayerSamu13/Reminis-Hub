package dev.reminishub.service;

import dev.reminishub.model.entity.*;
import dev.reminishub.model.enums.*;
import dev.reminishub.repository.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccesoGoogleTests {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final IdentidadExternaRepository identidades = mock(IdentidadExternaRepository.class);
    private final AccesoGoogle acceso = new AccesoGoogle(identidades, usuarios);
    private final OidcUser google = mock(OidcUser.class);

    private void primeraVisita(boolean verificado) {
        when(google.getSubject()).thenReturn("s-123");
        when(google.getEmail()).thenReturn("Persona@Example.invalid");
        when(google.getEmailVerified()).thenReturn(verificado);
    }

    @Test
    void primeraVisitaCreaUnColaboradorSinClaveLocal() {
        primeraVisita(true);
        when(usuarios.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertEquals("persona@example.invalid", acceso.cuentaLocal(google));
        var guardada = ArgumentCaptor.forClass(IdentidadExterna.class);
        verify(identidades).saveAndFlush(guardada.capture());
        var identidad = guardada.getValue();
        assertEquals("s-123", identidad.getSubject());
        assertEquals(ProveedorExterno.GOOGLE, identidad.getProveedor());
        assertNull(identidad.getUsuario().getPasswordHash());
        assertEquals(RolUsuario.COLABORADOR, identidad.getUsuario().getRol());
    }

    @Test
    void rechazaColisionConCuentaLocal() {
        primeraVisita(true);
        when(usuarios.existsByEmail("persona@example.invalid")).thenReturn(true);
        assertThrows(BadCredentialsException.class, () -> acceso.cuentaLocal(google));
        verify(identidades, never()).saveAndFlush(any());
    }

    @Test
    void altaExigeCorreoVerificado() {
        primeraVisita(false);
        assertThrows(BadCredentialsException.class, () -> acceso.cuentaLocal(google));
        verifyNoInteractions(usuarios);
    }

    @Test
    void reconoceVinculoExistentePorSubjectYRespetaDesactivacion() {
        when(google.getSubject()).thenReturn("s-123");
        var cuenta = new Usuario();
        cuenta.setEmail("original@example.invalid");
        cuenta.setActivo(true);
        var identidad = new IdentidadExterna();
        identidad.setUsuario(cuenta);
        when(identidades.findByProveedorAndSubject(ProveedorExterno.GOOGLE, "s-123")).thenReturn(Optional.of(identidad));
        assertEquals(cuenta.getEmail(), acceso.cuentaLocal(google));
        cuenta.setActivo(false);
        assertThrows(BadCredentialsException.class, () -> acceso.cuentaLocal(google));
        verifyNoInteractions(usuarios);
    }
}
