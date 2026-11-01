package dev.reminishub.model.entity;

import dev.reminishub.model.enums.EstadoProyecto;
import dev.reminishub.model.enums.OperacionBitacora;
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
@Table(name = "bitacora_proyecto")
public class BitacoraProyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación mantenida estricta para garantizar EntityNotFoundException en el ORM
    // en caso de un borrado físico, obligando a usar Soft Delete.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OperacionBitacora operacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoProyecto estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoProyecto estadoNuevo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_aplicacion_id")
    private Usuario usuarioAplicacion;

    @Column(name = "rol_bd_sesion", nullable = false, length = 63)
    private String rolBdSesion;

    @Column(length = 500)
    private String motivo;

    // Controlado exclusivamente por el trigger de la base de datos
    @Column(name = "registrado_en", insertable = false, updatable = false)
    private Instant registradoEn;

    public BitacoraProyecto() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Proyecto getProyecto() { return proyecto; }
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }

    public OperacionBitacora getOperacion() { return operacion; }
    public void setOperacion(OperacionBitacora operacion) { this.operacion = operacion; }

    public EstadoProyecto getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(EstadoProyecto estadoAnterior) { this.estadoAnterior = estadoAnterior; }

    public EstadoProyecto getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(EstadoProyecto estadoNuevo) { this.estadoNuevo = estadoNuevo; }

    public Usuario getUsuarioAplicacion() { return usuarioAplicacion; }
    public void setUsuarioAplicacion(Usuario usuarioAplicacion) { this.usuarioAplicacion = usuarioAplicacion; }

    public String getRolBdSesion() { return rolBdSesion; }
    public void setRolBdSesion(String rolBdSesion) { this.rolBdSesion = rolBdSesion; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public Instant getRegistradoEn() { return registradoEn; }
    public void setRegistradoEn(Instant registradoEn) { this.registradoEn = registradoEn; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BitacoraProyecto)) return false;
        BitacoraProyecto that = (BitacoraProyecto) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
