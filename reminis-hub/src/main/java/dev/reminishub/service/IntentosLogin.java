package dev.reminishub.service;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Ventana de cinco verificaciones por correo cada minuto, compartida por esta instancia. */
@Component
public class IntentosLogin {
    private record Ventana(long fin, int intentos) {}
    private final Map<String, Ventana> ventanas = new LinkedHashMap<>();
    private final Clock reloj;

    public IntentosLogin() { this(Clock.systemUTC()); }
    IntentosLogin(Clock reloj) { this.reloj = reloj; }

    /** Reserva un intento antes de BCrypt. Devuelve segundos de espera, o cero si procede. */
    public synchronized long reservar(String correo) {
        long ahora = reloj.instant().getEpochSecond();
        var entradas = ventanas.values().iterator();
        while (entradas.hasNext()) {
            if (entradas.next().fin() > ahora) break;
            entradas.remove();
        }
        Ventana actual = ventanas.get(correo);
        if (actual == null) {
            if (ventanas.size() >= 4096) return ventanas.values().iterator().next().fin() - ahora;
            ventanas.put(correo, new Ventana(ahora + 60, 1));
        } else {
            if (actual.intentos() >= 5) return actual.fin() - ahora;
            ventanas.put(correo, new Ventana(actual.fin(), actual.intentos() + 1));
        }
        return 0;
    }
}
