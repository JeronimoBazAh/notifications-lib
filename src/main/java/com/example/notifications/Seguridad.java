package com.example.notifications;

import java.util.Objects;

/**
 * Envuelve una credencial de modo que no pueda filtrarse a través de {@code toString()}, registros (logs) o visualizaciones de cadenas en depuradores.
 * El valor original solo es accesible mediante una llamada explícita a {@link #reveal()}, la cual es utilizada por los proveedores
 * en el último momento posible (al construir la cabecera Authorization).
 */
public final class Seguridad {
    private final String value;

    private Seguridad(String value) {
        this.value = value;
    }

    public static Seguridad of(String value) {
        Objects.requireNonNull(value, "secret value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Secret must not be blank");
        }
        return new Seguridad(value);
    }

    /** Lee el secreto de una variable de entorno. Falla de inmediato si no está presente. */
    public static Seguridad fromEnv(String variable) {
        String v = System.getenv(variable);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Environment variable '" + variable + "' is not set");
        }
        return new Seguridad(v);
    }

    public String reveal() {
        return value;
    }

    @Override
    public String toString() {
        return "Secret[****]";
    }
}
