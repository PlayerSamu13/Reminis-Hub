package dev.reminishub.dto;

import java.util.List;

/** Contrato de paginación independiente de la serialización interna de Spring Data. */
public record PaginaProyectos(List<ProyectoResumen> contenido, int pagina, int paginas, long total) { }
