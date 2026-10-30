package com.example.notifications;

import com.example.notifications.error.ErrorCode;
import com.example.notifications.event.NotificationEvent;
import com.example.notifications.event.NotificationListener;
import com.example.notifications.model.Notification;

import java.time.Instant;
import java.util.List;

/** Propaga los eventos del ciclo de vida a los oyentes. Si un oyente falla, se registra el error y se aísla dicho oyente. */
final class EventPublisher {
    private static final System.Logger LOG = System.getLogger(EventPublisher.class.getName());

    private final List<NotificationListener> listeners;

    EventPublisher(List<NotificationListener> listeners) {
        this.listeners = List.copyOf(listeners);
    }

    void publish(NotificationEvent.Type type, Notification n, String provider, int attempt, ErrorCode code, String detail) {
        if (listeners.isEmpty()) {
            return;
        }
        NotificationEvent event = new NotificationEvent(
                type, n.id(), n.channel(), provider, attempt, code, detail, n.metadata(), Instant.now());
        for (NotificationListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "Notification listener failed: " + e, e);
            }
        }
    }
}
