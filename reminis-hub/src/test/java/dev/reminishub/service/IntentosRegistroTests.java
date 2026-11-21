package dev.reminishub.service;

import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IntentosRegistroTests {
    @Test
    void limitaCadaIpYRecuperaAlExpirarSinDormir() {
        Clock reloj = mock(Clock.class);
        when(reloj.instant()).thenReturn(Instant.EPOCH);
        var limite = new IntentosRegistro(reloj);
        for (int i = 0; i < 5; i++) assertEquals(0, limite.reservar("uno"));
        assertEquals(60, limite.reservar("uno"));
        assertEquals(0, limite.reservar("dos"));
        when(reloj.instant()).thenReturn(Instant.ofEpochSecond(59));
        assertEquals(1, limite.reservar("uno"));
        when(reloj.instant()).thenReturn(Instant.ofEpochSecond(60));
        assertEquals(0, limite.reservar("uno"));
    }

    @Test
    void saturacionNoDesalojaIpBloqueadaYSeRecupera() {
        Clock reloj = mock(Clock.class);
        when(reloj.instant()).thenReturn(Instant.EPOCH);
        var limite = new IntentosRegistro(reloj);
        for (int i = 0; i < 4096; i++) assertEquals(0, limite.reservar("ip-" + i));
        assertEquals(60, limite.reservar("otra"));
        for (int i = 0; i < 4; i++) assertEquals(0, limite.reservar("ip-0"));
        assertEquals(60, limite.reservar("ip-0"));
        when(reloj.instant()).thenReturn(Instant.ofEpochSecond(60));
        assertEquals(0, limite.reservar("otra"));
    }
}
