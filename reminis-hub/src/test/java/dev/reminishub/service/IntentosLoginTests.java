package dev.reminishub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class IntentosLoginTests {
    @Test
    void limitaPorCorreoYPermiteReintentoAlTerminarLaVentana() {
        Clock reloj = mock(Clock.class);
        when(reloj.instant()).thenReturn(Instant.EPOCH);
        var limite = new IntentosLogin(reloj);
        for (int i = 0; i < 5; i++) assertEquals(0, limite.reservar("uno"));
        assertEquals(60, limite.reservar("uno"));
        assertEquals(0, limite.reservar("dos"));
        when(reloj.instant()).thenReturn(Instant.ofEpochSecond(59));
        assertEquals(1, limite.reservar("uno"));
        when(reloj.instant()).thenReturn(Instant.ofEpochSecond(60));
        assertEquals(0, limite.reservar("uno"));
    }
}
