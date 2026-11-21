package dev.reminishub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Coautor externo con referencia privada de autorización; no admite archivos ni IDs de cuentas ajenas. */
public class AutorForm {
    @NotBlank(message="{propuestas.autorNombre}") @Size(min=2,max=120,message="{propuestas.autorNombre}")
    private String nombrePublico;
    @NotBlank(message="{propuestas.evidencia}") @Size(max=255,message="{propuestas.evidencia}")
    private String referenciaEvidencia;
    public String getNombrePublico() { return nombrePublico; }
    public void setNombrePublico(String valor) { nombrePublico=valor==null?null:valor.strip(); }
    public String getReferenciaEvidencia() { return referenciaEvidencia; }
    public void setReferenciaEvidencia(String valor) { referenciaEvidencia=valor==null?null:valor.strip(); }
}
