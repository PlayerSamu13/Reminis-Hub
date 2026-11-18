package dev.reminishub.dto;

/**
 * Datos minimos de la sesion que puede ver la vista o la API.
 * Nunca incluye el hash ni el token: solo lo que el usuario ya conoce.
 */
public record SesionActual(String email, String nombre, String rol) { }
