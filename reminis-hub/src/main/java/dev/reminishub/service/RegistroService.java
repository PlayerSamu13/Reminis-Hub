package dev.reminishub.service;

import dev.reminishub.dto.RegistroForm;
import dev.reminishub.model.entity.Usuario;
import dev.reminishub.model.enums.RolUsuario;
import dev.reminishub.repository.UsuarioRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Crea cuentas locales con contraseña protegida y permisos de colaborador. */
@Service
public class RegistroService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwords;
    private final Validator validator;

    public RegistroService(UsuarioRepository usuarios, PasswordEncoder passwords, Validator validator) {
        this.usuarios = usuarios;
        this.passwords = passwords;
        this.validator = validator;
    }

    /**
     * Registra una cuenta activa. La transaccion revierte ante un conflicto o fallo de persistencia.
     * @param formulario datos normalizados mediante los setters del formulario
     * @throws CorreoRegistradoException si el correo ya existe, incluida una carrera de inserciones
     * @throws ConstraintViolationException si los datos incumplen el contrato de registro
     */
    @Transactional
    public void registrar(RegistroForm formulario) {
        var errores = validator.validate(formulario);
        if (!errores.isEmpty()) {
            throw new ConstraintViolationException(errores);
        }
        if (usuarios.existsByEmail(formulario.getEmail())) {
            throw new CorreoRegistradoException();
        }
        var usuario = new Usuario();
        usuario.setNombre(formulario.getNombre());
        usuario.setEmail(formulario.getEmail());
        usuario.setPasswordHash(passwords.encode(formulario.getContrasena()));
        usuario.setRol(RolUsuario.COLABORADOR);
        usuario.setActivo(true);
        try {
            // Flush hace visible la unicidad antes de salir del metodo transaccional.
            usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException error) {
            for (Throwable causa = error; causa != null; causa = causa.getCause()) {
                if (causa instanceof org.hibernate.exception.ConstraintViolationException constraint
                        && "23505".equals(constraint.getSQLState())
                        && "uq_usuario_email".equals(constraint.getConstraintName())) {
                    throw new CorreoRegistradoException();
                }
            }
            throw error;
        }
    }
}
