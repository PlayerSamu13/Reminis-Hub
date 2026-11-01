package dev.reminishub.model.entity;

import dev.reminishub.model.enums.ProveedorExterno;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "identidad_externa")
public class IdentidadExterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProveedorExterno proveedor;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(name = "correo_proveedor", length = 254)
    private String correoProveedor;

    @Column(name = "email_verificado")
    private Boolean emailVerificado;

    @Column(name = "vinculado_en", insertable = false, updatable = false)
    private Instant vinculadoEn;

    public IdentidadExterna() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public ProveedorExterno getProveedor() { return proveedor; }
    public void setProveedor(ProveedorExterno proveedor) { this.proveedor = proveedor; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getCorreoProveedor() { return correoProveedor; }
    public void setCorreoProveedor(String correoProveedor) { this.correoProveedor = correoProveedor; }

    public Boolean getEmailVerificado() { return emailVerificado; }
    public void setEmailVerificado(Boolean emailVerificado) { this.emailVerificado = emailVerificado; }

    public Instant getVinculadoEn() { return vinculadoEn; }
    public void setVinculadoEn(Instant vinculadoEn) { this.vinculadoEn = vinculadoEn; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdentidadExterna)) return false;
        IdentidadExterna that = (IdentidadExterna) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
