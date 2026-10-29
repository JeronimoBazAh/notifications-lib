package com.example.notifications.spi;

import com.example.notifications.error.SendException;
import com.example.notifications.model.ChannelType;
import com.example.notifications.model.Notification;

/**
 * Service Provider Interface: the ONLY thing a new provider (or a whole new channel) must implement.
 * <p>
 * Contract: on success return a {@link ProviderResponse}; on any failure throw {@link SendException},
 * stating whether it is retryable. Implementations must be thread-safe.
 *
 * @param <N> the notification type this provider can deliver
 */
public interface NotificationProvider<N extends Notification> {

    /** Stable, human-readable name used in results, events and logs (e.g. "sendgrid"). */
    String name();

    ChannelType channel();

    /** Lets the service dispatch type-safely without unchecked casts. */
    Class<N> notificationType();

    ProviderResponse send(N notification) throws SendException;
}
