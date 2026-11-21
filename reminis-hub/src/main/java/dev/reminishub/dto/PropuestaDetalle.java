package dev.reminishub.dto;

import java.util.List;

/** Detalle privado con evidencias de coautores; jamás se reutiliza en el catálogo público. */
public record PropuestaDetalle(PropuestaResumen resumen,String descripcion,String resumenTexto,
        String urlRepositorio,Integer anio,Integer semestre,Long categoriaId,List<Long> tecnologiaIds,
        List<AutorForm> autores,boolean propietario) {}
