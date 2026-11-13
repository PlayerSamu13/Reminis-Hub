package dev.reminishub.controller.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de comprobación del servicio.
 * Encargado de verificar el estado de la API.
 */

@RestController
@RequestMapping("/api/v1")
public class controllerREST {
    /**
     * Indica que la API esta en funcionamiento.
     * @return la cadena {@code Todo bien}
     */
    @GetMapping("ping")
    public String getMensaje() {
        return "Todo Bien";
    }
}
