package dev.reminishub.dto;

import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/** Campos editables de una propuesta; propietario, estado, actor y términos se fijan en el servidor. */
public class ProyectoForm {
    @NotBlank(message="{propuestas.nombre}") @Size(min=3,max=150,message="{propuestas.nombre}")
    private String nombre;
    @NotBlank(message="{propuestas.resumen}") @Size(min=30,max=280,message="{propuestas.resumen}")
    private String resumen;
    @NotBlank(message="{propuestas.descripcion}") @Size(min=20,max=4000,message="{propuestas.descripcion}")
    private String descripcion;
    @NotBlank(message="{propuestas.repositorio}") @Size(max=300,message="{propuestas.repositorio}")
    @Pattern(regexp="^https://github\\.com/(?!\\.{1,2}/)[A-Za-z0-9_.-]+/(?!\\.{1,2}$)(?!.*(?i:\\.git)$)[A-Za-z0-9_.-]+$",message="{propuestas.repositorio}")
    private String urlRepositorio;
    @NotNull(message="{propuestas.periodo}") @Min(value=2000,message="{propuestas.periodo}") @Max(value=2100,message="{propuestas.periodo}")
    private Integer anio;
    @NotNull(message="{propuestas.periodo}") @Min(value=1,message="{propuestas.periodo}") @Max(value=2,message="{propuestas.periodo}")
    private Integer semestre;
    @NotNull(message="{propuestas.categoria}") @Positive(message="{propuestas.categoria}")
    private Long categoriaId;
    @NotNull(message="{propuestas.tecnologias}") @Size(min=1,max=10,message="{propuestas.tecnologias}")
    private List<@NotNull @Positive Long> tecnologiaIds=new ArrayList<>();
    @NotNull @Size(max=9) @Valid
    private List<@NotNull @Valid AutorForm> autores=new ArrayList<>();
    @NotNull(message="{propuestas.terminos}") @AssertTrue(message="{propuestas.terminos}")
    private Boolean aceptaTerminos=false;
    @PositiveOrZero private Long version;

    private static String limpio(String valor) { return valor==null?null:valor.strip(); }
    public String getNombre(){return nombre;} public void setNombre(String v){nombre=limpio(v);}
    public String getResumen(){return resumen;} public void setResumen(String v){resumen=limpio(v);}
    public String getDescripcion(){return descripcion;} public void setDescripcion(String v){descripcion=limpio(v);}
    public String getUrlRepositorio(){return urlRepositorio;} public void setUrlRepositorio(String v){urlRepositorio=limpio(v);}
    public Integer getAnio(){return anio;} public void setAnio(Integer v){anio=v;}
    public Integer getSemestre(){return semestre;} public void setSemestre(Integer v){semestre=v;}
    public Long getCategoriaId(){return categoriaId;} public void setCategoriaId(Long v){categoriaId=v;}
    public List<Long> getTecnologiaIds(){return tecnologiaIds;} public void setTecnologiaIds(List<Long> v){tecnologiaIds=v;}
    public List<AutorForm> getAutores(){return autores;} public void setAutores(List<AutorForm> v){autores=v;}
    public Boolean getAceptaTerminos(){return aceptaTerminos;} public void setAceptaTerminos(Boolean v){aceptaTerminos=v;}
    public Long getVersion(){return version;} public void setVersion(Long v){version=v;}
}
