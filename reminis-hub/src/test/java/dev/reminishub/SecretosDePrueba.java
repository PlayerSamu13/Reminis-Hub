package dev.reminishub;

/**
 * Valores ficticios compartidos por las pruebas automatizadas.
 * No son secretos reales y no deben reutilizarse fuera de {@code src/test}.
 */
public final class SecretosDePrueba {
    /** Secreto HS256 de pruebas: 64 caracteres ASCII, muy por encima del minimo exigido. */
    public static final String JWT = "secreto-solo-para-pruebas-con-mas-de-32-bytes-1234567890";

    private SecretosDePrueba() { }
}
