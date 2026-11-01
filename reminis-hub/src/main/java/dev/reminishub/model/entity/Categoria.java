package dev.reminishub.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "categoria")
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false)
    private Boolean activa = true;

    @Column(nullable = false)
    private Short orden = 0;

    public Categoria() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long nuevoId) {
        this.id = nuevoId;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String nuevoCodigo) {
        this.codigo = nuevoCodigo;
    }

    public boolean getActiva() {
        return activa;
    }

    public void setActiva(boolean nuevoActiva) {
        this.activa = nuevoActiva;
    }

    public short getOrden() {
        return orden;
    }

    public void setOrden(short nuevoOrden) {
        this.orden = nuevoOrden;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof Categoria)) return false;
        Categoria categoria = (Categoria) o;
        return id != null && id.equals(categoria.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
