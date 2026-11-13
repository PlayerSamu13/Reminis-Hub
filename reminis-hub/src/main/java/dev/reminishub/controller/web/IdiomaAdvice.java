package dev.reminishub.controller.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.util.UriComponentsBuilder;

/** Conserva la ruta y los filtros al cambiar de idioma. */
@ControllerAdvice(basePackageClasses = controllerMVC.class)
public class IdiomaAdvice {
    @ModelAttribute("idiomaEs")
    public String espanol(HttpServletRequest request) { return enlace(request, "es"); }
    @ModelAttribute("idiomaEn")
    public String ingles(HttpServletRequest request) { return enlace(request, "en"); }
    private String enlace(HttpServletRequest request, String lang) {
        return UriComponentsBuilder.fromPath(request.getRequestURI()).query(request.getQueryString())
                .replaceQueryParam("lang", lang).build(true).toUriString();
    }
}
