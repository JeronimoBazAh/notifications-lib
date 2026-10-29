package com.example.notifications.event;

import com.example.notifications.error.ErrorCode;
import com.example.notifications.model.ChannelType;

import java.time.Instant;
import java.util.Map;

/**
 * Lifecycle event published by the service (Observer / pub-sub).
 *
 * @param attempt  overall attempt counter for this notification (across providers); 0 if none was made
 * @param metadata the notification's metadata (tenant, correlation id...) so listeners can correlate events
 */
public record NotificationEvent(
        Type type, String notificationId, ChannelType channel, String provider, int attempt,
        ErrorCode errorCode, String detail, Map<String, String> metadata, Instant timestamp) {

    public enum Type {
        /** Delivered to a provider successfully. detail = provider message id. */
        SENT,
        /** One attempt failed (it may be retried). detail = error message. */
        ATTEMPT_FAILED,
        /** Giving up on a provider and moving to the next one. detail = next provider name. */
        FAILOVER,
        /** Every attempt on every provider failed. */
        FAILED
    }
}
