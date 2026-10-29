package com.example.notifications;

import java.time.Duration;
import java.util.Objects;

/**
 * Retroceso exponencial aplicado POR PROVEEDOR y solo ante fallos que admiten reintento.
 *
 * @param maxAttempts número total de intentos, incluyendo el primero (1 = sin reintentos)
 * @param jitter      valor entre 0 y 1. Cada espera se reduce aleatoriamente hasta en esta proporción, de modo que, si varios clientes fallan simultáneamente, no todos realicen el reintento en el mismo instante (evitando el efecto «estampida» o *thundering herd*). 0 = determinista.
 */
public record RetryPolicy(int maxAttempts, Duration initialDelay, double multiplier, Duration maxDelay, double jitter) {

    public RetryPolicy {
        Objects.requireNonNull(initialDelay, "initialDelay");
        Objects.requireNonNull(maxDelay, "maxDelay");
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        if (initialDelay.isNegative()) throw new IllegalArgumentException("initialDelay must be >= 0");
        if (multiplier < 1) throw new IllegalArgumentException("multiplier must be >= 1");
        if (maxDelay.compareTo(initialDelay) < 0) throw new IllegalArgumentException("maxDelay must be >= initialDelay");
        if (jitter < 0 || jitter > 1) throw new IllegalArgumentException("jitter must be between 0 and 1");
    }

    /** Política determinista (sin fluctuaciones). */    public RetryPolicy(int maxAttempts, Duration initialDelay, double multiplier, Duration maxDelay) {
        this(maxAttempts, initialDelay, multiplier, maxDelay, 0.0);
    }

    public static RetryPolicy none() {
        return new RetryPolicy(1, Duration.ZERO, 1, Duration.ZERO, 0.0);
    }

    /** Duplica el retardo ante cada fallo, con un límite de 30 segundos (o el retardo inicial, si este fuera mayor). */
    public static RetryPolicy exponential(int maxAttempts, Duration initialDelay) {
        Duration cap = initialDelay.compareTo(Duration.ofSeconds(30)) > 0 ? initialDelay : Duration.ofSeconds(30);
        return new RetryPolicy(maxAttempts, initialDelay, 2.0, cap, 0.0);
    }

    public RetryPolicy withJitter(double jitter) {
        return new RetryPolicy(maxAttempts, initialDelay, multiplier, maxDelay, jitter);
    }

    /** Retraso base tras el intento fallido indicado (numerado a partir de 1), sin fluctuación (jitter). */
    public Duration delayAfter(int failedAttempt) {
        Duration d = Duration.ofMillis((long) (initialDelay.toMillis() * Math.pow(multiplier, failedAttempt - 1)));
        return d.compareTo(maxDelay) > 0 ? maxDelay : d;
    }

    /** Retraso con *jitter* aplicado. {@code randomUnit} debe estar en el intervalo [0, 1) (inyéctalo para mantener la determinista de las pruebas). */
    public Duration delayAfter(int failedAttempt, double randomUnit) {
        Duration base = delayAfter(failedAttempt);
        return Duration.ofMillis((long) (base.toMillis() * (1 - jitter * randomUnit)));
    }
}
