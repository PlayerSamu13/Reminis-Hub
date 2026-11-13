package dev.reminishub.dto;

import java.util.List;

/** Detalle de un proyecto publicado, sin identificadores de cuentas ni evidencias privadas. */
public record ProyectoDetalle(ProyectoResumen ficha, String descripcion, String asignatura,
                              String repositorio, List<String> autores) { }
