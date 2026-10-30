package com.example.notifications;

import com.example.notifications.error.NotificationException;
import com.example.notifications.model.TipoCanal;

import java.util.Objects;

/**
 * Outcome of a send: either {@link Sent} or {@link Failed}. Modelled as a sealed hierarchy so an impossible state
 * (e.g. "sent" carrying an error, or "failed" without one) cannot be represented.
 * <p>
 * Delivery failures are returned as DATA (never thrown), which keeps batch and async flows simple. Callers who
 * prefer exceptions can call {@link #orElseThrow()}; callers who prefer pattern matching can switch over the two cases:
 * <pre>{@code
 * switch (result) {
 *     case SendResult.Sent s   -> log(s.providerMessageId());
 *     case SendResult.Failed f -> alert(f.error().code());
 * }
 * }</pre>
 */
public sealed interface SendResult permits SendResult.Sent, SendResult.Failed {

    String notificationId();

    TipoCanal channel();

    /** Provider that delivered, or the last one tried on failure (null if none was involved). */
    String provider();

    /** Total attempts made, across providers. */
    int attempts();

    /** Provider-side id on success, {@code null} on failure. */
    String providerMessageId();

    /** The failure, or {@code null} on success. */
    NotificationException error();

    default boolean isSuccess() {
        return this instanceof Sent;
    }

    /** @return this result if it succeeded; @throws NotificationException the failure cause otherwise */
    SendResult orElseThrow();

    /** Delivered. {@code providerMessageId} may be null for providers that do not return one. */
    record Sent(String notificationId, TipoCanal channel, String provider, String providerMessageId, int attempts)
            implements SendResult {

        @Override
        public NotificationException error() {
            return null;
        }

        @Override
        public SendResult orElseThrow() {
            return this;
        }
    }

    /** Not delivered. {@code error} is always present. */
    record Failed(String notificationId, TipoCanal channel, String provider, int attempts, NotificationException error)
            implements SendResult {

        public Failed {
            Objects.requireNonNull(error, "error");
        }

        @Override
        public String providerMessageId() {
            return null;
        }

        @Override
        public SendResult orElseThrow() {
            throw error;
        }
    }
}
