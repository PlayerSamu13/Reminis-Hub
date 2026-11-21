package dev.reminishub.service;

import dev.reminishub.dto.*;
import dev.reminishub.model.entity.*;
import dev.reminishub.model.enums.*;
import dev.reminishub.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PropuestasServiceTests {
    PropuestasRepository proyectos=mock(PropuestasRepository.class);
    CategoriaRepository categorias=mock(CategoriaRepository.class);
    TecnologiaRepository tecnologias=mock(TecnologiaRepository.class);
    ActorProyecto actores=mock(ActorProyecto.class);
    EntityManager em=mock(EntityManager.class);
    ValidatorFactory fabrica;
    PropuestasService servicio;
    Usuario usuario;
    Proyecto proyecto;
    @BeforeEach void preparar(){
        fabrica=Validation.buildDefaultValidatorFactory();
        servicio=new PropuestasService(proyectos,categorias,tecnologias,actores,fabrica.getValidator(),em);
        usuario=new Usuario();usuario.setId(1L);usuario.setNombre("Autor propio");
        when(actores.establecer()).thenReturn(usuario);
        proyecto=new Proyecto();proyecto.setId(7L);proyecto.setVersion(2L);proyecto.setCargadoPor(usuario);
        when(proyectos.findById(7L)).thenReturn(Optional.of(proyecto));
    }
    @AfterEach void cerrar(){fabrica.close();}
    ProyectoForm valido(){
        var f=new ProyectoForm();f.setNombre("Proyecto valido");f.setResumen("Resumen suficientemente largo para validacion.");
        f.setDescripcion("Descripcion suficientemente larga de prueba.");f.setUrlRepositorio("https://github.com/prueba/proyecto");
        f.setAnio(2026);f.setSemestre(2);f.setCategoriaId(3L);f.setTecnologiaIds(List.of(4L));f.setAceptaTerminos(true);f.setVersion(2L);
        return f;
    }
    @Test void crearFijaEstadoPropietarioYConsentimiento(){
        var categoria=new Categoria();categoria.setId(3L);categoria.setActiva(true);
        var tecnologia=new Tecnologia();tecnologia.setId(4L);
        when(categorias.findById(3L)).thenReturn(Optional.of(categoria));
        when(tecnologias.findAllById(List.of(4L))).thenReturn(List.of(tecnologia));
        when(proyectos.saveAndFlush(any())).thenAnswer(inv->{Proyecto p=inv.getArgument(0);p.setId(9L);return p;});
        assertEquals(9L,servicio.crear(valido()));
        var captor=org.mockito.ArgumentCaptor.forClass(Proyecto.class);verify(proyectos).saveAndFlush(captor.capture());
        Proyecto guardado=captor.getValue();assertEquals(EstadoProyecto.PENDIENTE_REVISION,guardado.getEstado());
        assertSame(usuario,guardado.getCargadoPor());assertFalse(guardado.getEsFicticio());
        assertEquals(MedioAutorizacion.CUENTA_PROPIA,guardado.getAutores().getFirst().getMedioAutorizacion());
        assertSame(usuario,guardado.getAutores().getFirst().getUsuario());assertNotNull(guardado.getTerminosAceptadosEn());
    }
    @Test void otroPropietarioNoPuedeLeerNiRetirar(){
        var ajeno=new Usuario();ajeno.setId(99L);proyecto.setCargadoPor(ajeno);
        assertEquals(404,assertThrows(ResponseStatusException.class,()->servicio.detalle(7L)).getStatusCode().value());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->servicio.retirar(7L,2)).getStatusCode().value());
        verify(proyectos,never()).saveAndFlush(any());
    }
    @Test void colaboradorNoModera(){
        assertThrows(AccessDeniedException.class,()->servicio.decidir(7L,2,true,null));
        assertThrows(AccessDeniedException.class,()->servicio.listar(0,true));
        verify(proyectos,never()).saveAndFlush(any());
    }
    @Test void versionObsoletaYRetiradoNoSeEscriben(){
        assertEquals(409,assertThrows(ResponseStatusException.class,()->servicio.retirar(7L,1)).getStatusCode().value());
        proyecto.setEstado(EstadoProyecto.RETIRADO);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->servicio.retirar(7L,2)).getStatusCode().value());
        verify(proyectos,never()).saveAndFlush(any());
    }
    @Test void publicadoNoSeEdita(){
        proyecto.setEstado(EstadoProyecto.PUBLICADO);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->servicio.actualizar(7L,valido())).getStatusCode().value());
        verify(proyectos,never()).saveAndFlush(any());verifyNoInteractions(em);
    }
    @Test void rechazoRequiereMotivoYNoReenvia(){
        usuario.setRol(RolUsuario.ADMIN);
        assertEquals(400,assertThrows(ResponseStatusException.class,()->servicio.decidir(7L,2,false," ")).getStatusCode().value());
        servicio.decidir(7L,2,false," Falta evidencia ");
        assertEquals(EstadoProyecto.RECHAZADO,proyecto.getEstado());assertEquals("Falta evidencia",proyecto.getMotivoDecision());
    }
    @Test void repositorioGitYSinConsentimientoSeRechazan(){
        var conGit=valido();
        conGit.setUrlRepositorio("https://github.com/prueba/repo.git");
        assertThrows(jakarta.validation.ConstraintViolationException.class,()->servicio.crear(conGit));
        var sinConsentimiento=valido();
        sinConsentimiento.setAceptaTerminos(false);
        assertThrows(jakarta.validation.ConstraintViolationException.class,()->servicio.crear(sinConsentimiento));
        verifyNoInteractions(actores,proyectos);
    }
    @Test void listaNulaYRutasEspecialesSeRechazanAntesDePersistir(){
        var nulo=valido();nulo.setAutores(null);
        assertThrows(jakarta.validation.ConstraintViolationException.class,()->servicio.crear(nulo));
        for(String url:List.of("https://github.com/../repo","https://github.com/owner/..",
                "https://github.com/owner/repo.GIT","http://github.com/owner/repo","https://github.com/owner/repo/")){
            var formulario=valido();formulario.setUrlRepositorio(url);
            assertThrows(jakarta.validation.ConstraintViolationException.class,()->servicio.crear(formulario));
        }
        verifyNoInteractions(actores,proyectos);
    }
}
