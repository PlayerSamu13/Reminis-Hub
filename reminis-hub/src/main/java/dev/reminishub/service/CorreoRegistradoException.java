package dev.reminishub.service;

/** Conflicto de registro que puede mostrarse sin exponer detalles de persistencia. */
public class CorreoRegistradoException extends RuntimeException {
    public CorreoRegistradoException() {
        super("El correo ya pertenece a una cuenta.");
    }
}
