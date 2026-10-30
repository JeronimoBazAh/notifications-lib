package com.example.notifications.model;

import java.util.Locale;
import java.util.Objects;

/**
 * Identifica un canal de entrega. Se ha definido deliberadamente como un `record` y NO como un `enum`: un `enum` es cerrado, por lo que añadir
 * un canal (Slack, WhatsApp...) obligaría a modificar el código de la biblioteca. Con un `record`, cualquiera puede definir uno nuevo.
 */
public record ChanelType(String name) {
    public static final ChanelType EMAIL = new ChanelType("EMAIL");
    public static final ChanelType SMS = new ChanelType("SMS");
    public static final ChanelType PUSH = new ChanelType("PUSH");
    public static final ChanelType SLACK = new ChanelType("SLACK");

    public ChanelType {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Channel name must not be blank");
        }
        name = name.trim().toUpperCase(Locale.ROOT);
    }

    public static ChanelType of(String name) {
        return new ChanelType(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
