package dev.reminishub.controller.api;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import java.sql.SQLException;

/** Respuestas de escritura sin SQL, credenciales, trazas ni referencias privadas. */
@RestControllerAdvice(assignableTypes=PropuestasApiController.class)
public class PropuestasApiErrors {
    public record ErrorPropuesta(int estado,String codigo){}
    @ExceptionHandler({ConstraintViolationException.class,MethodArgumentNotValidException.class})
    ResponseEntity<ErrorPropuesta> validacion(Exception error){return respuesta(400,"DATOS_INVALIDOS");}
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class,jakarta.persistence.OptimisticLockException.class})
    ResponseEntity<ErrorPropuesta> concurrencia(Exception error){return respuesta(409,"CONFLICTO_VERSION");}
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorPropuesta> permiso(Exception error){return respuesta(403,"ACCESO_DENEGADO");}
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorPropuesta> dominio(ResponseStatusException error){
        int estado=error.getStatusCode().value();
        return respuesta(estado,switch(estado){case 404->"NO_ENCONTRADO";case 409->"CONFLICTO";default->"DATOS_INVALIDOS";});
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorPropuesta> integridad(DataIntegrityViolationException error){
        for(Throwable causa=error;causa!=null;causa=causa.getCause()) {
            if(causa instanceof SQLException sql) {
                if("23505".equals(sql.getSQLState())) return respuesta(409,"CONFLICTO_UNICIDAD");
                if("23514".equals(sql.getSQLState()) || "23503".equals(sql.getSQLState())) return respuesta(400,"DATOS_INVALIDOS");
            }
        }
        return respuesta(500,"ERROR_INTERNO");
    }
    private ResponseEntity<ErrorPropuesta> respuesta(int estado,String codigo){
        return ResponseEntity.status(estado).body(new ErrorPropuesta(estado,codigo));
    }
}
