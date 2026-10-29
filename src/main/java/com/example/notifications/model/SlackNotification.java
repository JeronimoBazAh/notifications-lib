package com.example.notifications.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.example.notifications.model.Validation.copyOf;
import static com.example.notifications.model.Validation.hasText;

/**
 * Slack message. {@code target} is a channel id or name ("#alerts"); it is NOT called "channel" because
 * {@link Notification#channel()} already means the delivery channel type. {@code threadTs} replies in a thread.
 */
public record SlackNotification(
        String id, Instant createdAt, String target, String text, String threadTs, Map<String, String> metadata)
        implements Notification {

    /** Slack truncates messages longer than 40,000 characters. */
    public static final int MAX_TEXT_LENGTH = 40_000;

    public SlackNotification {
        id = id == null ? UUID.randomUUID().toString() : id;
        createdAt = createdAt == null ? Instant.now() : createdAt;
        metadata = copyOf(metadata);

        Validation v = new Validation();
        v.require(hasText(target), "'target' (channel id or name) is required");
        v.require(hasText(text), "'text' must not be blank");
        v.require(text == null || text.length() <= MAX_TEXT_LENGTH, "'text' exceeds " + MAX_TEXT_LENGTH + " characters");
        v.throwIfAny("SlackNotification");
    }

    @Override
    public ChannelType channel() {
        return ChannelType.SLACK;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "SlackNotification[id=" + id + "]";
    }

    public static final class Builder {
        private String id, target, text, threadTs;
        private final Map<String, String> metadata = new LinkedHashMap<>();

        public Builder id(String v) { id = v; return this; }
        public Builder target(String v) { target = v; return this; }
        public Builder text(String v) { text = v; return this; }
        public Builder threadTs(String v) { threadTs = v; return this; }
        public Builder metadata(String k, String v) { metadata.put(k, v); return this; }

        public SlackNotification build() {
            return new SlackNotification(id, null, target, text, threadTs, metadata);
        }
    }
}
