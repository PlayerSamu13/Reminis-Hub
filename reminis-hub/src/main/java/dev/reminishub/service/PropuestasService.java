package dev.reminishub.service;

import dev.reminishub.dto.*;
import dev.reminishub.model.entity.*;
import dev.reminishub.model.enums.*;
import dev.reminishub.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Coordina autorización, validación y escritura auditada en una única transacción JPA. */
@Service
@Transactional
public class PropuestasService {
    public static final String TERMINOS="RH-2026-01";
    private final PropuestasRepository proyectos;
    private final CategoriaRepository categorias;
    private final TecnologiaRepository tecnologias;
    private final ActorProyecto actores;
    private final Validator validator;
    private final EntityManager em;

    public PropuestasService(PropuestasRepository proyectos,CategoriaRepository categorias,
            TecnologiaRepository tecnologias,ActorProyecto actores,Validator validator,EntityManager em) {
        this.proyectos=proyectos; this.categorias=categorias; this.tecnologias=tecnologias;
        this.actores=actores; this.validator=validator; this.em=em;
    }

    /** Crea una propuesta pendiente y su consentimiento; nunca acepta rol, actor o estado del cliente. */
    public Long crear(ProyectoForm formulario) {
        validar(formulario);
        Usuario actor=actores.establecer();
        Proyecto proyecto=new Proyecto();
        proyecto.setCargadoPor(actor);
        proyecto.setEstado(EstadoProyecto.PENDIENTE_REVISION);
        completar(proyecto,formulario,actor);
        return proyectos.saveAndFlush(proyecto).getId();
    }

    /** Modifica solo propuestas pendientes/rechazadas del propietario; versión obsoleta produce 409. */
    public void actualizar(long id,ProyectoForm formulario) {
        validar(formulario);
        Usuario actor=actores.establecer();
        Proyecto p=exigir(id,actor);
        editable(p,actor);
        version(p,formulario.getVersion());
        // También protege cambios exclusivos de relaciones contra una edición concurrente.
        em.lock(p,LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        p.getAutores().clear();
        proyectos.flush(); // Elimina primero los órdenes previos para no colisionar con UNIQUE.
        completar(p,formulario,actor);
        proyectos.saveAndFlush(p);
    }

    /** Retiro lógico final por propietario o administrador; conserva el historial. */
    public void retirar(long id,long version) {
        Usuario actor=actores.establecer();
        Proyecto p=exigir(id,actor);
        version(p,version);
        if(p.getEstado()==EstadoProyecto.RETIRADO) throw conflicto();
        p.setEstado(EstadoProyecto.RETIRADO);
        proyectos.saveAndFlush(p);
    }

    /** Solo ADMIN resuelve una propuesta pendiente; el rechazo exige motivo visible al propietario. */
    public void decidir(long id,long version,boolean aprobar,String motivo) {
        Usuario actor=actores.establecer();
        administrador(actor);
        Proyecto p=exigir(id,actor);
        version(p,version);
        if(p.getEstado()!=EstadoProyecto.PENDIENTE_REVISION) throw conflicto();
        String limpio=motivo==null?"":motivo.strip();
        if(!aprobar && (limpio.isBlank() || limpio.length()>500)) throw invalido();
        if(aprobar && (p.getAutores().isEmpty() || p.getTecnologias().isEmpty())) throw invalido();
        p.setEstado(aprobar?EstadoProyecto.PUBLICADO:EstadoProyecto.RECHAZADO);
        p.setMotivoDecision(aprobar?null:limpio);
        proyectos.saveAndFlush(p);
    }

    /** Páginas acotadas de diez registros, propias o pendientes para moderar. */
    @Transactional(readOnly=true)
    public List<PropuestaResumen> listar(int pagina,boolean moderacion) {
        if(pagina<0 || pagina>1000) throw invalido();
        Usuario actor=actores.establecer();
        if(moderacion) administrador(actor);
        var limite=PageRequest.of(pagina,10);
        return (moderacion?proyectos.findByEstadoOrderByIdDesc(EstadoProyecto.PENDIENTE_REVISION,limite)
                :proyectos.findByCargadoPorIdOrderByIdDesc(actor.getId(),limite)).stream().map(this::resumen).toList();
    }

    /** Detalle privado autorizado; las referencias de consentimiento no salen en API pública. */
    @Transactional(readOnly=true)
    public PropuestaDetalle detalle(long id) {
        Usuario actor=actores.establecer();
        Proyecto p=exigir(id,actor);
        return new PropuestaDetalle(resumen(p),p.getDescripcion(),p.getResumen(),p.getUrlRepositorio(),
                p.getAnio().intValue(),p.getSemestre().intValue(),p.getCategoria().getId(),
                p.getTecnologias().stream().map(Tecnologia::getId).sorted().toList(),externos(p),
                p.getCargadoPor().getId().equals(actor.getId()));
    }

    /** Datos para editar sin aceptar una nueva autorización por defecto. */
    @Transactional(readOnly=true)
    public ProyectoForm formulario(long id) {
        Usuario actor=actores.establecer(); Proyecto p=exigir(id,actor); editable(p,actor);
        var f=new ProyectoForm(); f.setNombre(p.getNombre()); f.setResumen(p.getResumen());
        f.setDescripcion(p.getDescripcion()); f.setUrlRepositorio(p.getUrlRepositorio());
        f.setAnio(p.getAnio().intValue()); f.setSemestre(p.getSemestre().intValue());
        f.setCategoriaId(p.getCategoria().getId()); f.setVersion(p.getVersion());
        f.setTecnologiaIds(p.getTecnologias().stream().map(Tecnologia::getId).toList());
        f.setAutores(externos(p)); return f;
    }

    private void validar(ProyectoForm f) {
        if(f==null) throw invalido();
        var errores=validator.validate(f);
        if(!errores.isEmpty()) throw new ConstraintViolationException(errores);
        if(new HashSet<>(f.getTecnologiaIds()).size()!=f.getTecnologiaIds().size()) throw invalido();
    }
    private void completar(Proyecto p,ProyectoForm f,Usuario actor) {
        Categoria categoria=categorias.findById(f.getCategoriaId()).filter(Categoria::getActiva).orElseThrow(PropuestasService::invalido);
        var seleccion=tecnologias.findAllById(f.getTecnologiaIds());
        if(seleccion.size()!=f.getTecnologiaIds().size()) throw invalido();
        p.setNombre(f.getNombre()); p.setResumen(f.getResumen()); p.setDescripcion(f.getDescripcion());
        p.setUrlRepositorio(f.getUrlRepositorio()); p.setAnio(f.getAnio().shortValue()); p.setSemestre(f.getSemestre().shortValue());
        p.setCategoria(categoria); p.setTecnologias(new HashSet<>(seleccion));
        p.setTerminosAceptadosEn(Instant.now()); p.setTerminosVersion(TERMINOS);
        var propio=new ProyectoAutor(); propio.setUsuario(actor); propio.setNombrePublico(actor.getNombre());
        propio.setOrden((short)1); propio.setMedioAutorizacion(MedioAutorizacion.CUENTA_PROPIA);
        propio.setAutorizadoEn(Instant.now()); propio.setVersionAutorizacion(TERMINOS); p.agregarAutor(propio);
        short orden=2;
        for(AutorForm fuente:f.getAutores()) {
            var autor=new ProyectoAutor(); autor.setNombrePublico(fuente.getNombrePublico()); autor.setOrden(orden++);
            autor.setMedioAutorizacion(MedioAutorizacion.AUTORIZACION_DOCUMENTADA);
            autor.setReferenciaEvidencia(fuente.getReferenciaEvidencia()); autor.setAutorizadoEn(Instant.now());
            autor.setVersionAutorizacion(TERMINOS); p.agregarAutor(autor);
        }
    }
    private Proyecto exigir(long id,Usuario actor) {
        var p=proyectos.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!p.getCargadoPor().getId().equals(actor.getId()) && actor.getRol()!=RolUsuario.ADMIN)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return p;
    }
    private static void editable(Proyecto p,Usuario actor) {
        if(!p.getCargadoPor().getId().equals(actor.getId())) throw new AccessDeniedException("Solo propietario");
        if(p.getEstado()!=EstadoProyecto.PENDIENTE_REVISION && p.getEstado()!=EstadoProyecto.RECHAZADO) throw conflicto();
    }
    private static void administrador(Usuario actor) {
        if(actor.getRol()!=RolUsuario.ADMIN) throw new AccessDeniedException("Solo administrador");
    }
    private static void version(Proyecto p,Long esperada) {
        if(esperada==null || !esperada.equals(p.getVersion())) throw conflicto();
    }
    private PropuestaResumen resumen(Proyecto p) {
        return new PropuestaResumen(p.getId(),p.getNombre(),p.getEstado().name(),p.getVersion(),p.getMotivoDecision());
    }
    private List<AutorForm> externos(Proyecto p) {
        return p.getAutores().stream().filter(a->a.getMedioAutorizacion()==MedioAutorizacion.AUTORIZACION_DOCUMENTADA)
                .sorted(Comparator.comparing(ProyectoAutor::getOrden)).map(a->{var f=new AutorForm();
                    f.setNombrePublico(a.getNombrePublico()); f.setReferenciaEvidencia(a.getReferenciaEvidencia()); return f;}).toList();
    }
    private static ResponseStatusException invalido(){return new ResponseStatusException(HttpStatus.BAD_REQUEST);}
    private static ResponseStatusException conflicto(){return new ResponseStatusException(HttpStatus.CONFLICT);}
}
