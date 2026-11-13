package dev.reminishub.dto;

import java.util.List;

/** Datos públicos de una tarjeta; nunca incluye cuentas ni evidencia de autorización. */
public record ProyectoResumen(Long id, String nombre, String resumen, String categoria,
                              short anio, short semestre, boolean ficticio, List<String> tecnologias) {
    public String periodo() { return anio + "-" + (semestre == 1 ? "I" : "II"); }
}
