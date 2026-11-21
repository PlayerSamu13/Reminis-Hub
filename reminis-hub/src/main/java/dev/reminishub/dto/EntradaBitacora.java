package dev.reminishub.dto;

import java.time.Instant;

/** Hecho auditado visible al administrador, sin credenciales ni referencias de consentimiento. */
public record EntradaBitacora(long id, long proyectoId, String operacion, String estadoAnterior,
        String estadoNuevo, Long actorId, String motivo, Instant registradoEn) { }
