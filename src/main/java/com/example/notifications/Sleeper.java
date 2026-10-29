package com.example.notifications;

import java.time.Duration;

/** Abstraction over waiting between retries so tests don't actually sleep. */
@FunctionalInterface
public interface Sleeper {
    Sleeper THREAD = d -> Thread.sleep(d.toMillis());

    void sleep(Duration duration) throws InterruptedException;
}
