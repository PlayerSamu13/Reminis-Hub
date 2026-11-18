package dev.reminishub.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Datos editables del registro. Rol, estado e identificador se asignan en el servidor. */
public class RegistroForm {
    @NotBlank(message = "{registro.nombreInvalido}")
    @Size(min = 2, max = 120, message = "{registro.nombreInvalido}")
    private String nombre;

    @NotBlank(message = "{registro.correoInvalido}")
    @Email(message = "{registro.correoInvalido}")
    @Size(max = 254, message = "{registro.correoInvalido}")
    private String email;

    @NotBlank(message = "{registro.claveInvalida}")
    @Size(min = 15, max = 72, message = "{registro.claveInvalida}")
    private String contrasena;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre == null ? null : nombre.strip(); }
    public String getEmail() { return email; }
    public void setEmail(String email) {
        this.email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    /** BCrypt limita la entrada por bytes UTF-8, que pueden superar la cantidad de caracteres. */
    @AssertTrue(message = "{registro.claveBytes}")
    public boolean isContrasenaCompatible() {
        return contrasena == null || contrasena.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
