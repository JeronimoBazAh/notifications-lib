package com.example.notifications.model;

import java.util.Locale;
import java.util.Objects;

/**
 * Identifies a delivery channel. Deliberately a record and NOT an enum: an enum is closed, so adding
 * a channel (Slack, WhatsApp...) would force editing library code. With a record anyone can define one.
 */
public record ChannelType(String name) {
    public static final ChannelType EMAIL = new ChannelType("EMAIL");
    public static final ChannelType SMS = new ChannelType("SMS");
    public static final ChannelType PUSH = new ChannelType("PUSH");
    public static final ChannelType SLACK = new ChannelType("SLACK");

    public ChannelType {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Channel name must not be blank");
        }
        name = name.trim().toUpperCase(Locale.ROOT);
    }

    public static ChannelType of(String name) {
        return new ChannelType(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
