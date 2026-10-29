package com.example.notifications.model;

import java.util.Locale;
import java.util.Objects;

/**
 * Identifica un canal de entrega. Se ha definido deliberadamente como un `record` y NO como un `enum`: un `enum` es cerrado, por lo que añadir
 * un canal (Slack, WhatsApp...) obligaría a modificar el código de la biblioteca. Con un `record`, cualquiera puede definir uno nuevo.
 */
public record TipoCanal(String name) {
    public static final TipoCanal EMAIL = new TipoCanal("EMAIL");
    public static final TipoCanal SMS = new TipoCanal("SMS");
    public static final TipoCanal PUSH = new TipoCanal("PUSH");
    public static final TipoCanal SLACK = new TipoCanal("SLACK");

    public TipoCanal {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Channel name must not be blank");
        }
        name = name.trim().toUpperCase(Locale.ROOT);
    }

    public static TipoCanal of(String name) {
        return new TipoCanal(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
