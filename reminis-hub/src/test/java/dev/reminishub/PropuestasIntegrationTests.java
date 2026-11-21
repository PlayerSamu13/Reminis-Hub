package dev.reminishub;

import dev.reminishub.dto.AutorForm;
import dev.reminishub.dto.ProyectoForm;
import dev.reminishub.service.PropuestasService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

/** Servicios reales, JPA y triggers PostgreSQL; todos los fixtures se revierten con la transacción. */
@SpringBootTest(properties="reminishub.jwt.secret="+SecretosDePrueba.JWT)
@Transactional
@EnabledIfEnvironmentVariable(named="CATALOGO_DB_TESTS",matches="true")
class PropuestasIntegrationTests {
    @DynamicPropertySource static void baseAislada(DynamicPropertyRegistry registro){
        String url=System.getenv("DB_URL");
        if(!"jdbc:postgresql://localhost:5432/reminishub_test".equals(url))
            throw new IllegalStateException("Solo se permite reminishub_test");
        registro.add("spring.datasource.url",()->url);
    }
    @Autowired PropuestasService propuestas;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    String marca,correo,correoAdmin;
    long usuario,admin,categoria,tecnologia;
    @BeforeEach void datos(){
        marca=UUID.randomUUID().toString().replace("-","");
        correo=marca+"@example.invalid";correoAdmin="admin-"+correo;
        usuario=jdbc.queryForObject("insert into usuario(email,nombre) values (?,?) returning id",Long.class,correo,"Autor prueba");
        admin=jdbc.queryForObject("insert into usuario(email,nombre,rol) values (?,?,'ADMIN') returning id",Long.class,correoAdmin,"Admin prueba");
        categoria=jdbc.queryForObject("select id from categoria where activa order by id limit 1",Long.class);
        tecnologia=jdbc.queryForObject("select id from tecnologia order by id limit 1",Long.class);
        autenticar(correo,"COLABORADOR");
    }
    @AfterEach void limpiar(){SecurityContextHolder.clearContext();}
    void autenticar(String email,String rol){
        var jwt=Jwt.withTokenValue("ficticio").header("alg","HS256").subject(email).claim("rol",rol).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,List.of()));
    }
    ProyectoForm formulario(){
        var f=new ProyectoForm();f.setNombre("Propuesta "+marca);f.setResumen("Resumen de prueba suficientemente largo para el contrato.");
        f.setDescripcion("Descripción de prueba para comprobar persistencia y permisos.");
        f.setUrlRepositorio("https://github.com/pruebas/"+marca);f.setAnio(2026);f.setSemestre(2);
        f.setCategoriaId(categoria);f.setTecnologiaIds(List.of(tecnologia));f.setAceptaTerminos(true);
        return f;
    }
    @Test void altaEdicionRechazoYRetiroConservanActorYRelaciones(){
        long id=propuestas.crear(formulario());
        var inicial=propuestas.detalle(id);
        assertEquals("PENDIENTE_REVISION",inicial.resumen().estado());
        assertTrue(inicial.propietario());
        assertEquals(usuario,jdbc.queryForObject("select usuario_aplicacion_id from bitacora_proyecto where proyecto_id=?",Long.class,id));
        var editar=propuestas.formulario(id);editar.setAceptaTerminos(true);
        var coautor=new AutorForm();coautor.setNombrePublico("Coautor externo");coautor.setReferenciaEvidencia("CONSENTIMIENTO-PRUEBA");
        editar.setAutores(List.of(coautor));propuestas.actualizar(id,editar);
        em.flush();em.clear();
        var actualizado=propuestas.detalle(id);
        assertTrue(actualizado.resumen().version()>inicial.resumen().version());
        assertEquals(1,actualizado.autores().size());
        assertEquals(2,jdbc.queryForObject("select count(*) from proyecto_autor where proyecto_id=?",Integer.class,id));
        autenticar(correoAdmin,"ADMIN");
        propuestas.decidir(id,actualizado.resumen().version(),false,"Falta evidencia verificable");
        em.flush();em.clear();
        assertEquals(admin,jdbc.queryForObject("select usuario_aplicacion_id from bitacora_proyecto where proyecto_id=? and estado_nuevo='RECHAZADO'",Long.class,id));
        autenticar(correo,"COLABORADOR");
        var rechazado=propuestas.detalle(id);
        assertEquals("Falta evidencia verificable",rechazado.resumen().motivoDecision());
        propuestas.retirar(id,rechazado.resumen().version());
        assertEquals("RETIRADO",propuestas.detalle(id).resumen().estado());
        assertEquals(3,jdbc.queryForObject("select count(*) from bitacora_proyecto where proyecto_id=?",Integer.class,id));
    }
    @Test void publicadoNoEsEditable(){
        long id=propuestas.crear(formulario());
        var editar=propuestas.formulario(id);editar.setAceptaTerminos(true);
        autenticar(correoAdmin,"ADMIN");
        propuestas.decidir(id,propuestas.detalle(id).resumen().version(),true,null);
        em.flush();em.clear();
        assertEquals("PUBLICADO",propuestas.detalle(id).resumen().estado());
        autenticar(correo,"COLABORADOR");
        editar.setVersion(propuestas.detalle(id).resumen().version());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->propuestas.actualizar(id,editar)).getStatusCode().value());
    }
    @Test void otroColaboradorNoLeeLaPropuesta(){
        long id=propuestas.crear(formulario());
        String ajeno="ajeno-"+correo;
        jdbc.update("insert into usuario(email,nombre) values (?,?)",ajeno,"Otro colaborador");
        autenticar(ajeno,"COLABORADOR");
        assertEquals(404,assertThrows(ResponseStatusException.class,()->propuestas.detalle(id)).getStatusCode().value());
    }
    @Test void versionObsoletaNoRetira(){
        long id=propuestas.crear(formulario());
        long version=propuestas.detalle(id).resumen().version();
        assertEquals(409,assertThrows(ResponseStatusException.class,()->propuestas.retirar(id,version+1)).getStatusCode().value());
    }
}
