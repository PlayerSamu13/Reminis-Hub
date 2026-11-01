package dev.reminishub.model.entity;

import dev.reminishub.model.enums.MedioAutorizacion;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "proyecto_autor")
public class ProyectoAutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "nombre_publico", nullable = false, length = 120)
    private String nombrePublico;

    @Column(nullable = false)
    private Short orden;

    @Column(name = "autorizado_en")
    private Instant autorizadoEn;

    @Column(name = "version_autorizacion", length = 30)
    private String versionAutorizacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_autorizacion", nullable = false, length = 30)
    private  MedioAutorizacion medioAutorizacion;

    @Column(name = "referencia_evidencia", length = 255)
    private String referenciaEvidencia;

    public ProyectoAutor() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Proyecto getProyecto() {
        return proyecto;
    }

    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getNombrePublico() {
        return nombrePublico;
    }

    public void setNombrePublico(String nombrePublico) {
        this.nombrePublico = nombrePublico;
    }

    public Short getOrden() {
        return orden;
    }

    public void setOrden(Short orden) {
        this.orden = orden;
    }

    public Instant getAutorizadoEn() {
        return autorizadoEn;
    }

    public void setAutorizadoEn(Instant autorizadoEn) {
        this.autorizadoEn = autorizadoEn;
    }

    public String getVersionAutorizacion() {
        return versionAutorizacion;
    }

    public void setVersionAutorizacion(String versionAutorizacion) {
        this.versionAutorizacion = versionAutorizacion;
    }

    public MedioAutorizacion getMedioAutorizacion() {
        return medioAutorizacion;
    }

    public void setMedioAutorizacion(MedioAutorizacion medioAutorizacion) {
        this.medioAutorizacion = medioAutorizacion;
    }

    public String getReferenciaEvidencia() {
        return referenciaEvidencia;
    }

    public void setReferenciaEvidencia(String referenciaEvidencia) {
        this.referenciaEvidencia = referenciaEvidencia;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProyectoAutor)) return false;
        ProyectoAutor proyectoAutor = (ProyectoAutor) o;
        return id != null && id.equals(proyectoAutor.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
