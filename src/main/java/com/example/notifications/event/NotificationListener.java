package com.example.notifications.event;

/**
 * Subscribe to notification lifecycle events (metrics, audit, DLQ...).
 * Listener exceptions are caught and logged: a faulty listener never breaks delivery.
 */
@FunctionalInterface
public interface NotificationListener {
    void onEvent(NotificationEvent event);
}
