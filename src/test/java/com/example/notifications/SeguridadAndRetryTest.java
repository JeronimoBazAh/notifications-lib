package com.example.notifications;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class SeguridadAndRetryTest {
    @Test
    void secretNeverLeaksInToString() {
        Seguridad s = Seguridad.of("super-secret");
        assertFalse(s.toString().contains("super-secret"));
        assertEquals("super-secret", s.reveal());
    }

    @Test
    void blankSecretIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Seguridad.of(" "));
    }

    @Test
    void missingEnvironmentVariableFailsFast() {
        assertThrows(IllegalStateException.class, () -> Seguridad.fromEnv("DEFINITELY_NOT_SET_VAR_12345"));
    }

    @Test
    void backoffDoublesAndIsCapped() {
        RetryPolicy p = new RetryPolicy(5, Duration.ofSeconds(1), 2, Duration.ofSeconds(3));
        assertEquals(Duration.ofSeconds(1), p.delayAfter(1));
        assertEquals(Duration.ofSeconds(2), p.delayAfter(2));
        assertEquals(Duration.ofSeconds(3), p.delayAfter(3)); // 4s capped
    }

    @Test
    void invalidPolicyIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(0, Duration.ZERO, 1, Duration.ZERO));
    }
}
