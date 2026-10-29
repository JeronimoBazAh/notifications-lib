package com.example.notifications;

import java.util.Objects;

/**
 * Wraps a credential so it cannot leak through {@code toString()}, logs or debuggers' string views.
 * The raw value is only reachable through the explicit {@link #reveal()} call, which providers use
 * at the very last moment (when building the Authorization header).
 */
public final class Secret {
    private final String value;

    private Secret(String value) {
        this.value = value;
    }

    public static Secret of(String value) {
        Objects.requireNonNull(value, "secret value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Secret must not be blank");
        }
        return new Secret(value);
    }

    /** Reads the secret from an environment variable. Fails fast if it is missing. */
    public static Secret fromEnv(String variable) {
        String v = System.getenv(variable);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Environment variable '" + variable + "' is not set");
        }
        return new Secret(v);
    }

    public String reveal() {
        return value;
    }

    @Override
    public String toString() {
        return "Secret[****]";
    }
}
