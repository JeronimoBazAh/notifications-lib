package com.example.notifications.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.example.notifications.model.Validation.copyOf;
import static com.example.notifications.model.Validation.hasText;
import static com.example.notifications.model.Validation.isE164;

/** SMS message. {@code from} is optional: providers fall back to their configured sender / messaging service. */
public record SmsNotification(
        String id, Instant createdAt, String to, String from, String body, Map<String, String> metadata)
        implements Notification {

    /** Twilio's documented maximum body length. */
    public static final int MAX_BODY_LENGTH = 1600;

    public SmsNotification {
        id = id == null ? UUID.randomUUID().toString() : id;
        createdAt = createdAt == null ? Instant.now() : createdAt;
        metadata = copyOf(metadata);

        Validation v = new Validation();
        v.require(isE164(to), "'to' must be a phone number in E.164 format (e.g. +5493515551234)");
        v.require(from == null || isE164(from) || (hasText(from) && !from.contains(" ")),
                "'from' must be an E.164 number or an alphanumeric sender id without spaces");
        v.require(hasText(body), "'body' must not be blank");
        v.require(body == null || body.length() <= MAX_BODY_LENGTH, "'body' exceeds " + MAX_BODY_LENGTH + " characters");
        v.throwIfAny("SmsNotification");
    }

    @Override
    public ChannelType channel() {
        return ChannelType.SMS;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "SmsNotification[id=" + id + "]";
    }

    public static final class Builder {
        private String id, to, from, body;
        private final Map<String, String> metadata = new LinkedHashMap<>();

        public Builder id(String id) { this.id = id; return this; }
        public Builder to(String to) { this.to = to; return this; }
        public Builder from(String from) { this.from = from; return this; }
        public Builder body(String body) { this.body = body; return this; }
        public Builder metadata(String key, String value) { this.metadata.put(key, value); return this; }

        public SmsNotification build() {
            return new SmsNotification(id, null, to, from, body, metadata);
        }
    }
}
