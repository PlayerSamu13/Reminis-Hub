package dev.reminishub;

import dev.reminishub.dto.RegistroForm;
import dev.reminishub.model.entity.Usuario;
import dev.reminishub.model.enums.RolUsuario;
import dev.reminishub.repository.UsuarioRepository;
import dev.reminishub.service.CorreoRegistradoException;
import dev.reminishub.service.RegistroService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Reglas aisladas de registro. Los casos de persistencia real estan en RegistroIntegrationTests. */
class RegistroServiceTests {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final RegistroService service = new RegistroService(usuarios, encoder, FACTORY.getValidator());

    @AfterAll
    static void cerrarValidator() { FACTORY.close(); }

    static RegistroForm formulario() {
        var form = new RegistroForm();
        form.setNombre("  Estudiante de prueba  ");
        form.setEmail("  Prueba@Example.Invalid  ");
        form.setContrasena("Frase ficticia para pruebas 123");
        return form;
    }

    @Test
    void normalizaYGuardaHashConRolFijado() {
        var form = formulario();
        service.registrar(form);
        var captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).saveAndFlush(captor.capture());
        var usuario = captor.getValue();
        assertEquals("prueba@example.invalid", usuario.getEmail());
        assertEquals("Estudiante de prueba", usuario.getNombre());
        assertEquals(RolUsuario.COLABORADOR, usuario.getRol());
        assertTrue(usuario.getActivo());
        assertNotEquals(form.getContrasena(), usuario.getPasswordHash());
        assertTrue(encoder.matches(form.getContrasena(), usuario.getPasswordHash()));
    }

    @Test
    void correoExistenteNoSeGuarda() {
        when(usuarios.existsByEmail("prueba@example.invalid")).thenReturn(true);
        assertThrows(CorreoRegistradoException.class, () -> service.registrar(formulario()));
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void unicidadPosteriorALaConsultaSeTraduce() {
        var constraint = new org.hibernate.exception.ConstraintViolationException(
                "conflicto simulado", new SQLException("duplicado", "23505"), "uq_usuario_email");
        when(usuarios.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("persistencia", constraint));
        assertThrows(CorreoRegistradoException.class, () -> service.registrar(formulario()));
    }

    @Test
    void otraRestriccionNoSeConfundeConCorreoDuplicado() {
        var constraint = new org.hibernate.exception.ConstraintViolationException(
                "conflicto simulado", new SQLException("check", "23514"), "ck_usuario_nombre");
        var error = new DataIntegrityViolationException("persistencia", constraint);
        when(usuarios.saveAndFlush(any())).thenThrow(error);
        assertSame(error, assertThrows(DataIntegrityViolationException.class, () -> service.registrar(formulario())));
    }

    @Test
    void unicodeMayorA72BytesSeRechazaAntesDeConsultar() {
        var form = formulario();
        form.setContrasena("á".repeat(37));
        assertThrows(ConstraintViolationException.class, () -> service.registrar(form));
        verifyNoInteractions(usuarios);
    }
}
