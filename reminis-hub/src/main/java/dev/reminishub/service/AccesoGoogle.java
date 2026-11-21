package dev.reminishub.service;

import dev.reminishub.model.entity.IdentidadExterna;
import dev.reminishub.model.entity.Usuario;
import dev.reminishub.model.enums.ProveedorExterno;
import dev.reminishub.model.enums.RolUsuario;
import dev.reminishub.repository.IdentidadExternaRepository;
import dev.reminishub.repository.UsuarioRepository;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resuelve la cuenta local a partir de una identidad Google ya validada por Spring Security. */
@Service
public class AccesoGoogle {
    private final IdentidadExternaRepository identidades;
    private final UsuarioRepository usuarios;

    public AccesoGoogle(IdentidadExternaRepository identidades, UsuarioRepository usuarios) {
        this.identidades = identidades;
        this.usuarios = usuarios;
    }

    /** Un alta nueva crea la cuenta y su vínculo en la misma transacción. */
    @Transactional
    public String cuentaLocal(OidcUser google) {
        String sub = google.getSubject();
        if (sub == null || sub.isBlank() || sub.length() > 255) throw denegado();
        var previa = identidades.findByProveedorAndSubject(ProveedorExterno.GOOGLE, sub);
        if (previa.isPresent()) {
            Usuario cuenta = previa.get().getUsuario();
            if (!Boolean.TRUE.equals(cuenta.getActivo())) throw denegado();
            return cuenta.getEmail();
        }
        if (!Boolean.TRUE.equals(google.getEmailVerified()) || google.getEmail() == null) throw denegado();
        String email = google.getEmail().strip().toLowerCase(Locale.ROOT);
        if (email.length() > 254 || !email.matches("[^@\\s]+@[^@\\s]+") || usuarios.existsByEmail(email)) throw denegado();
        String nombre = google.getFullName() == null ? "" : google.getFullName().strip();
        if (nombre.codePointCount(0, nombre.length()) < 2 || nombre.length() > 120) nombre = "Colaborador";
        var cuenta = new Usuario();
        cuenta.setEmail(email);
        cuenta.setNombre(nombre);
        cuenta.setRol(RolUsuario.COLABORADOR);
        cuenta.setActivo(true);
        var identidad = new IdentidadExterna();
        identidad.setUsuario(usuarios.save(cuenta));
        identidad.setProveedor(ProveedorExterno.GOOGLE);
        identidad.setSubject(sub);
        identidad.setCorreoProveedor(email);
        identidad.setEmailVerificado(true);
        identidades.saveAndFlush(identidad);
        return email;
    }

    private static BadCredentialsException denegado() {
        return new BadCredentialsException("No se pudo iniciar sesión con Google.");
    }
}
