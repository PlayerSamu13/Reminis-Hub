package dev.reminishub;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class controllerMVC {
    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("mensaje", "¡Thank You Bleach!");
        return "index";
    }
}
