package dev.reminishub.model.entity;

import dev.reminishub.model.enums.EstadoProyecto;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "proyecto")
public class Proyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 280)
    private String resumen;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "url_repositorio", nullable = false, length = 300)
    private String urlRepositorio;

    @Column(name = "url_demo", length = 300)
    private String urlDemo;

    @Column(nullable = false)
    private Short anio;

    @Column(nullable = false)
    private Short semestre;

    @Column(length = 120)
    private String asignatura;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoProyecto estado = EstadoProyecto.PENDIENTE_REVISION;

    @Column(name = "motivo_decision", length = 500)
    private String motivoDecision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cargado_por", nullable = false)
    private Usuario cargadoPor;

    @Column(name = "terminos_aceptados_en", nullable = false)
    private Instant terminosAceptadosEn;

    @Column(name = "terminos_version", nullable = false, length = 30)
    private String terminosVersion;

    @Column(name = "es_ficticio", nullable = false, updatable = false)
    private Boolean esFicticio = false;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private Instant actualizadoEn;

    @OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 20)
    private List<ProyectoAutor> autores = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "proyecto_tecnologia",
            joinColumns = @JoinColumn(name = "proyecto_id"),
            inverseJoinColumns = @JoinColumn(name = "tecnologia_id")
    )
    private Set<Tecnologia> tecnologias = new HashSet<>();

    // Constructor vacío
    public Proyecto() {
    }

    // Getters y Setters explícitos
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getResumen() { return resumen; }
    public void setResumen(String resumen) { this.resumen = resumen; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getUrlRepositorio() { return urlRepositorio; }
    public void setUrlRepositorio(String urlRepositorio) { this.urlRepositorio = urlRepositorio; }

    public String getUrlDemo() { return urlDemo; }
    public void setUrlDemo(String urlDemo) { this.urlDemo = urlDemo; }

    public Short getAnio() { return anio; }
    public void setAnio(Short anio) { this.anio = anio; }

    public Short getSemestre() { return semestre; }
    public void setSemestre(Short semestre) { this.semestre = semestre; }

    public String getAsignatura() { return asignatura; }
    public void setAsignatura(String asignatura) { this.asignatura = asignatura; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public EstadoProyecto getEstado() { return estado; }
    public void setEstado(EstadoProyecto estado) { this.estado = estado; }

    public String getMotivoDecision() { return motivoDecision; }
    public void setMotivoDecision(String motivoDecision) { this.motivoDecision = motivoDecision; }

    public Usuario getCargadoPor() { return cargadoPor; }
    public void setCargadoPor(Usuario cargadoPor) { this.cargadoPor = cargadoPor; }

    public Instant getTerminosAceptadosEn() { return terminosAceptadosEn; }
    public void setTerminosAceptadosEn(Instant terminosAceptadosEn) { this.terminosAceptadosEn = terminosAceptadosEn; }

    public String getTerminosVersion() { return terminosVersion; }
    public void setTerminosVersion(String terminosVersion) { this.terminosVersion = terminosVersion; }

    public Boolean getEsFicticio() { return esFicticio; }
    public void setEsFicticio(Boolean esFicticio) { this.esFicticio = esFicticio; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }

    public Instant getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Instant actualizadoEn) { this.actualizadoEn = actualizadoEn; }

    public List<ProyectoAutor> getAutores() { return autores; }

    public void setAutores(List<ProyectoAutor> nuevosAutores) {
        this.autores.clear();
        if (nuevosAutores != null) {
            nuevosAutores.forEach(this::agregarAutor);
        }
    }

    public void agregarAutor(ProyectoAutor autor) {
        if (autor != null) {
            this.autores.add(autor);
            autor.setProyecto(this);
        }
    }

    public void removerAutor(ProyectoAutor autor) {
        if (autor != null) {
            this.autores.remove(autor);
            autor.setProyecto(null);
        }
    }

    public Set<Tecnologia> getTecnologias() { return tecnologias; }

    public void setTecnologias(Set<Tecnologia> tecnologias) {
        this.tecnologias.clear();
        if (tecnologias != null) {
            this.tecnologias.addAll(tecnologias);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Proyecto)) return false;
        Proyecto proyecto = (Proyecto) o;
        return id != null && id.equals(proyecto.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
