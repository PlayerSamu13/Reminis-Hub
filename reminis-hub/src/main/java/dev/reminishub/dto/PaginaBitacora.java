package dev.reminishub.dto;

import java.util.List;

/** Página estable de veinte entradas; siguiente indica si existe al menos otra entrada. */
public record PaginaBitacora(List<EntradaBitacora> contenido, int pagina, boolean siguiente) { }
