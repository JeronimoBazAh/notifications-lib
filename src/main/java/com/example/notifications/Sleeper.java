package com.example.notifications;

import java.time.Duration;

/** Abstracción de la espera entre reintentos, de modo que las pruebas no realicen pausas reales. */
@FunctionalInterface
public interface Sleeper {
    Sleeper THREAD = d -> Thread.sleep(d.toMillis());

    void sleep(Duration duration) throws InterruptedException;
}
