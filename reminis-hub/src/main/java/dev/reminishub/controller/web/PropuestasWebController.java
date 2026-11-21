package dev.reminishub.controller.web;

import dev.reminishub.dto.ProyectoForm;
import dev.reminishub.service.CatalogoService;
import dev.reminishub.service.PropuestasService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Presentación y seguimiento privado; las reglas de propiedad y moderación pertenecen al servicio. */
@Controller
public class PropuestasWebController {
    private final PropuestasService propuestas;
    private final CatalogoService catalogo;

    public PropuestasWebController(PropuestasService propuestas, CatalogoService catalogo) {
        this.propuestas = propuestas;
        this.catalogo = catalogo;
    }

    /** No permite vincular propietario, estado, actor ni campos internos desde el cliente. */
    @InitBinder("propuesta")
    void campos(WebDataBinder binder) {
        binder.setAllowedFields("nombre", "resumen", "descripcion", "urlRepositorio", "anio", "semestre",
                "categoriaId", "tecnologiaIds", "autores[*].nombrePublico", "autores[*].referenciaEvidencia",
                "aceptaTerminos", "version");
        binder.setAutoGrowCollectionLimit(20);
    }

    @GetMapping("/mis-propuestas")
    public String propias(@RequestParam(defaultValue = "0") int pagina, Model modelo) {
        return listar(pagina, false, modelo);
    }

    @GetMapping("/admin/propuestas")
    public String pendientes(@RequestParam(defaultValue = "0") int pagina, Model modelo) {
        return listar(pagina, true, modelo);
    }

    private String listar(int pagina, boolean moderacion, Model modelo) {
        modelo.addAttribute("propuestas", propuestas.listar(pagina, moderacion));
        modelo.addAttribute("pagina", pagina);
        modelo.addAttribute("moderacion", moderacion);
        return "propuestas-lista";
    }

    @GetMapping("/propuestas/nueva")
    public String nueva(Model modelo) {
        modelo.addAttribute("propuesta", new ProyectoForm());
        return prepararFormulario(null, modelo);
    }

    @GetMapping("/propuestas/{id}/editar")
    public String editar(@PathVariable long id, Model modelo) {
        modelo.addAttribute("propuesta", propuestas.formulario(id));
        return prepararFormulario(id, modelo);
    }

    private String prepararFormulario(Long id, Model modelo) {
        modelo.addAttribute("propuestaId", id);
        modelo.addAttribute("categorias", catalogo.categoriasDisponibles());
        modelo.addAttribute("tecnologias", catalogo.tecnologiasDisponibles());
        return "propuestas-formulario";
    }

    /** Conserva los datos válidos ante errores y usa PRG después de una escritura exitosa. */
    @PostMapping("/propuestas/nueva")
    public String crear(@Valid @ModelAttribute("propuesta") ProyectoForm formulario, BindingResult errores,
                        Model modelo, HttpServletResponse respuesta, RedirectAttributes flash) {
        if (errores.hasErrors()) {
            respuesta.setStatus(400);
            return prepararFormulario(null, modelo);
        }
        try {
            Long id = propuestas.crear(formulario);
            flash.addFlashAttribute("operacionCorrecta", true);
            return "redirect:/propuestas/" + id;
        } catch (ResponseStatusException error) {
            return errorFormulario(error, null, modelo, respuesta);
        } catch (org.springframework.dao.DataIntegrityViolationException | org.springframework.dao.OptimisticLockingFailureException error) {
            return conflictoFormulario(null, modelo, respuesta);
        }
    }

    @PostMapping("/propuestas/{id}/editar")
    public String actualizar(@PathVariable long id, @Valid @ModelAttribute("propuesta") ProyectoForm formulario,
                            BindingResult errores, Model modelo, HttpServletResponse respuesta,
                            RedirectAttributes flash) {
        if (errores.hasErrors()) {
            respuesta.setStatus(400);
            return prepararFormulario(id, modelo);
        }
        try {
            propuestas.actualizar(id, formulario);
            flash.addFlashAttribute("operacionCorrecta", true);
            return "redirect:/propuestas/" + id;
        } catch (ResponseStatusException error) {
            return errorFormulario(error, id, modelo, respuesta);
        } catch (org.springframework.dao.DataIntegrityViolationException | org.springframework.dao.OptimisticLockingFailureException error) {
            return conflictoFormulario(id, modelo, respuesta);
        }
    }

    private String conflictoFormulario(Long id, Model modelo, HttpServletResponse respuesta) {
        respuesta.setStatus(409);
        modelo.addAttribute("conflicto", true);
        modelo.addAttribute("errorOperacion", true);
        return prepararFormulario(id, modelo);
    }

    private String errorFormulario(ResponseStatusException error, Long id, Model modelo, HttpServletResponse respuesta) {
        if (error.getStatusCode().value() != 400 && error.getStatusCode().value() != 409) throw error;
        respuesta.setStatus(error.getStatusCode().value());
        modelo.addAttribute("conflicto", error.getStatusCode().value() == 409);
        modelo.addAttribute("errorOperacion", true);
        return prepararFormulario(id, modelo);
    }

    @GetMapping("/propuestas/{id}")
    public String detalle(@PathVariable long id, Authentication autenticacion, Model modelo) {
        modelo.addAttribute("propuesta", propuestas.detalle(id));
        modelo.addAttribute("administrador", autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        return "propuestas-detalle";
    }

    @PostMapping("/propuestas/{id}/retirar")
    public String retirar(@PathVariable long id, @RequestParam long version, RedirectAttributes flash) {
        propuestas.retirar(id, version);
        flash.addFlashAttribute("operacionCorrecta", true);
        return "redirect:/propuestas/" + id;
    }

    @PostMapping("/admin/propuestas/{id}/decision")
    public String decidir(@PathVariable long id, @RequestParam long version, @RequestParam boolean aprobar,
                          @RequestParam(defaultValue = "") String motivo,
                          @RequestParam(defaultValue = "false") boolean revisionConfirmada, RedirectAttributes flash) {
        if (aprobar && !revisionConfirmada) throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);
        propuestas.decidir(id, version, aprobar, motivo);
        flash.addFlashAttribute("operacionCorrecta", true);
        return "redirect:/propuestas/" + id;
    }

    /** Nunca muestra causas SQL ni excepciones internas a los usuarios. */
    @ExceptionHandler(ResponseStatusException.class)
    String error(ResponseStatusException error, Model modelo, HttpServletResponse respuesta) {
        respuesta.setStatus(error.getStatusCode().value());
        modelo.addAttribute("codigo", error.getStatusCode().value());
        return "propuestas-error";
    }

    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class,
            org.springframework.dao.OptimisticLockingFailureException.class})
    String conflicto(Model modelo, HttpServletResponse respuesta) {
        respuesta.setStatus(409);
        modelo.addAttribute("codigo", 409);
        return "propuestas-error";
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    String invalidos(Model modelo, HttpServletResponse respuesta) {
        respuesta.setStatus(400);
        modelo.addAttribute("codigo", 400);
        return "propuestas-error";
    }
}
