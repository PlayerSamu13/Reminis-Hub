package dev.reminishub.dto;

/** Agregado por periodo, categoría y estado; ficticios es un subconjunto de total. */
public record FilaReporte(int anio, String categoria, String estado, long total, long ficticios) { }
