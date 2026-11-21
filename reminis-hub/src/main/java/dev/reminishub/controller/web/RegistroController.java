package dev.reminishub.controller.web;

import dev.reminishub.dto.RegistroForm;
import dev.reminishub.service.CorreoRegistradoException;
import dev.reminishub.service.RegistroService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Formulario publico para crear una cuenta de colaborador mediante HTTPS y CSRF. */
@Controller
public class RegistroController {
    private final RegistroService registro;
    private final dev.reminishub.service.IntentosRegistro intentos;

    public RegistroController(RegistroService registro, dev.reminishub.service.IntentosRegistro intentos) {
        this.registro = registro;
        this.intentos = intentos;
    }

    @InitBinder("registro")
    void camposPermitidos(WebDataBinder binder) {
        binder.setAllowedFields("nombre", "email", "contrasena");
    }

    /** @return vista del formulario, con la confirmacion flash tras un registro exitoso */
    @GetMapping("/registro")
    public String formulario(Model model) {
        model.addAttribute("registro", new RegistroForm());
        return "registro";
    }

    /**
     * @param formulario campos publicos del registro
     * @param errores resultado de validacion del formulario
     * @param response permite devolver 400 o 409 conservando mensajes junto a los campos
     * @param redirect confirmacion de un solo uso, sin datos personales
     * @return formulario con errores o redireccion para evitar reenvio al recargar
     */
    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("registro") RegistroForm formulario,
                            BindingResult errores, HttpServletResponse response,
                            RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest peticion,
                            Model modelo) {
        long espera = intentos.reservar(peticion.getRemoteAddr());
        if (espera > 0) {
            formulario.setContrasena(null);
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(espera));
            modelo.addAttribute("esperaRegistro", espera);
            return "registro";
        }
        if (errores.hasErrors()) {
            formulario.setContrasena(null);
            response.setStatus(400);
            return "registro";
        }
        try {
            registro.registrar(formulario);
        } catch (CorreoRegistradoException error) {
            errores.rejectValue("email", "registro.correoOcupado");
            formulario.setContrasena(null);
            response.setStatus(409);
            return "registro";
        }
        formulario.setContrasena(null);
        redirect.addFlashAttribute("registroCreado", true);
        return "redirect:/registro";
    }
}
