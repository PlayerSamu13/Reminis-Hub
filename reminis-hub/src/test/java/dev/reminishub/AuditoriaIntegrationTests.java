package dev.reminishub;

import dev.reminishub.service.ActorProyecto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** PostgreSQL real: escritura y actor en una conexión; rollback y reutilización del pool de tamaño uno. */
@SpringBootTest(properties = {"spring.datasource.hikari.maximum-pool-size=1",
        "reminishub.jwt.secret=" + SecretosDePrueba.JWT})
@EnabledIfEnvironmentVariable(named = "CATALOGO_DB_TESTS", matches = "true")
class AuditoriaIntegrationTests {
    @DynamicPropertySource
    static void baseAislada(DynamicPropertyRegistry registro) {
        String url = System.getenv("DB_URL");
        if (!"jdbc:postgresql://localhost:5432/reminishub_test".equals(url))
            throw new IllegalStateException("Solo se permite reminishub_test");
        registro.add("spring.datasource.url", () -> url);
    }

    @Autowired ActorProyecto actor;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transacciones;

    @AfterEach void limpiarIdentidad() { SecurityContextHolder.clearContext(); }

    @Test
    void actorExigeTransaccionExistente() {
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class, actor::establecer);
    }

    @Test
    void auditaYRevierteSinContaminarConexionReutilizada() {
        var tx = new TransactionTemplate(transacciones);
        String unico = java.util.UUID.randomUUID().toString().replace("-", "");
        String correo = unico + "@example.invalid";
        long[] ids = new long[3];
        tx.executeWithoutResult(estado -> {
            ids[0] = ((Number) em.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue();
            ids[1] = ((Number) em.createNativeQuery("insert into usuario(email,nombre) values (:email,'Prueba auditoria') returning id")
                    .setParameter("email", correo).getSingleResult()).longValue();
            var jwt = Jwt.withTokenValue("ficticio").header("alg", "HS256").subject(correo)
                    .claim("rol", "COLABORADOR").build();
            SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, java.util.List.of()));
            assertEquals(ids[1], actor.establecer().getId());
            ids[2] = ((Number) em.createNativeQuery("""
                    insert into proyecto(nombre,resumen,descripcion,url_repositorio,anio,semestre,
                        categoria_id,cargado_por,terminos_aceptados_en,terminos_version)
                    values ('Proyecto de prueba','Resumen valido para verificar auditoria transaccional',
                        'Descripcion suficiente para probar el rollback',:url,2026,2,
                        (select id from categoria order by id limit 1),:actor,now(),'v1') returning id
                    """).setParameter("url", "https://github.com/prueba/" + unico)
                    .setParameter("actor", ids[1]).getSingleResult()).longValue();
            Number registrado = (Number) em.createNativeQuery(
                    "select usuario_aplicacion_id from bitacora_proyecto where proyecto_id=:id")
                    .setParameter("id", ids[2]).getSingleResult();
            assertEquals(ids[1], registrado.longValue());
            estado.setRollbackOnly();
        });
        tx.executeWithoutResult(estado -> {
            assertEquals(ids[0], ((Number) em.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue());
            assertNull(em.createNativeQuery("select nullif(current_setting('app.actor_user_id',true),'')").getSingleResult());
            assertEquals(0L, ((Number) em.createNativeQuery("select count(*) from proyecto where id=:id")
                    .setParameter("id", ids[2]).getSingleResult()).longValue());
            assertEquals(0L, ((Number) em.createNativeQuery("select count(*) from bitacora_proyecto where proyecto_id=:id")
                    .setParameter("id", ids[2]).getSingleResult()).longValue());
            assertEquals(0L, ((Number) em.createNativeQuery("select count(*) from usuario where id=:id")
                    .setParameter("id", ids[1]).getSingleResult()).longValue());
        });
    }
}
