package dev.reminishub.dto;

/** Estado privado y versión de concurrencia, disponibles al propietario o al administrador. */
public record PropuestaResumen(Long id,String nombre,String estado,Long version,String motivoDecision) {}
