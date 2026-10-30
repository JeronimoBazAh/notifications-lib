package com.example.notifications.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.example.notifications.model.Validation.copyOf;
import static com.example.notifications.model.Validation.hasText;

/**
 * Notificación push para móviles. Un único destino: un {@code deviceToken} o un {@code topic}.
 * {@code data} es un mapa de cadenas (String a String), ya que FCM (así como las claves personalizadas de APNs) solo admite valores de tipo cadena.
 */
public record PushNotification(
        String id, Instant createdAt, String deviceToken, String topic, String title, String body,
        Map<String, String> data, Map<String, String> metadata) implements Notification {

    public PushNotification {
        id = id == null ? UUID.randomUUID().toString() : id;
        createdAt = createdAt == null ? Instant.now() : createdAt;
        data = copyOf(data);
        metadata = copyOf(metadata);

        Validation v = new Validation();
        v.require(hasText(deviceToken) ^ hasText(topic), "exactly one of 'deviceToken' or 'topic' is required");
        v.require(hasText(title) || hasText(body), "'title' or 'body' is required");
        data.keySet().forEach(k -> v.require(hasText(k), "'data' keys must not be blank"));
        v.throwIfAny("PushNotification");
    }

    @Override
    public ChanelType channel() {
        return ChanelType.PUSH;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "PushNotification[id=" + id + "]";
    }

    public static final class Builder {
        private String id, deviceToken, topic, title, body;
        private final Map<String, String> data = new LinkedHashMap<>();
        private final Map<String, String> metadata = new LinkedHashMap<>();

        public Builder id(String id) { this.id = id; return this; }
        public Builder deviceToken(String deviceToken) { this.deviceToken = deviceToken; return this; }
        public Builder topic(String topic) { this.topic = topic; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder body(String body) { this.body = body; return this; }
        public Builder data(String key, String value) { this.data.put(key, value); return this; }
        public Builder metadata(String key, String value) { this.metadata.put(key, value); return this; }

        public PushNotification build() {
            return new PushNotification(id, null, deviceToken, topic, title, body, data, metadata);
        }
    }
}
