package dev.reminishub.service;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Limita el registro a cinco intentos por IP y minuto en esta instancia, con memoria acotada. */
@Component
public class IntentosRegistro {
    private record Ventana(long fin, int intentos) {}
    private final Map<String, Ventana> ventanas = new HashMap<>();
    private final Clock reloj;

    public IntentosRegistro() { this(Clock.systemUTC()); }
    IntentosRegistro(Clock reloj) { this.reloj = reloj; }

    /** Reserva antes de BCrypt; devuelve cero o los segundos hasta poder reintentar. */
    public synchronized long reservar(String ip) {
        long ahora = reloj.instant().getEpochSecond();
        ventanas.values().removeIf(v -> v.fin() <= ahora);
        Ventana actual = ventanas.get(ip);
        if (actual == null) {
            // No desalojar entradas vigentes: permitiría evadir el límite llenando el mapa.
            if (ventanas.size() >= 4096)
                return ventanas.values().stream().mapToLong(Ventana::fin).min().orElseThrow() - ahora;
            ventanas.put(ip, new Ventana(ahora + 60, 1));
        } else {
            if (actual.intentos() >= 5) return actual.fin() - ahora;
            ventanas.put(ip, new Ventana(actual.fin(), actual.intentos() + 1));
        }
        return 0;
    }
}
