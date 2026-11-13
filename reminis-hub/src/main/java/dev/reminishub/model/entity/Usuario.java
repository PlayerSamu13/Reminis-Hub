package dev.reminishub.model.entity;

import dev.reminishub.model.enums.RolUsuario;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private RolUsuario rol = RolUsuario.COLABORADOR;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private Instant actualizadoEn;

    public Usuario() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long nuevoId) {
        this.id = nuevoId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String nuevoEmail) {
        this.email = nuevoEmail;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nuevoNombre) {
        this.nombre = nuevoNombre;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String nuevaPasswordHash) {
        this.passwordHash = nuevaPasswordHash;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public void setRol(RolUsuario nuevoRol) {
        this.rol = nuevoRol;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean nuevoActivo) {
        this.activo = nuevoActivo;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant nuevoCreadoEn) {
        this.creadoEn = nuevoCreadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(Instant nuevoActualizadoEn) {
        this.actualizadoEn = nuevoActualizadoEn;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof Usuario)) return false;
        Usuario usuario = (Usuario) o;
        return id != null && id.equals(usuario.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
