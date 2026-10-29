package com.example.notifications;

import com.example.notifications.error.NotificationException;
import com.example.notifications.model.ChannelType;

/**
 * Outcome of a send. Delivery failures are returned as DATA (never thrown), which keeps batch and async
 * flows simple. Callers who prefer exceptions can call {@link #orElseThrow()}.
 *
 * @param provider          provider that delivered, or the last one tried on failure (null if none)
 * @param providerMessageId provider-side id on success, null on failure
 * @param attempts          total attempts made, across providers
 * @param error             null on success
 */
public record SendResult(
        String notificationId, ChannelType channel, Status status, String provider,
        String providerMessageId, int attempts, NotificationException error) {

    public enum Status { SENT, FAILED }

    public boolean isSuccess() {
        return status == Status.SENT;
    }

    /** @throws NotificationException the failure cause, if the send failed */
    public SendResult orElseThrow() {
        if (error != null) {
            throw error;
        }
        return this;
    }

    static SendResult success(String id, ChannelType channel, String provider, String messageId, int attempts) {
        return new SendResult(id, channel, Status.SENT, provider, messageId, attempts, null);
    }

    static SendResult failure(String id, ChannelType channel, String provider, int attempts, NotificationException error) {
        return new SendResult(id, channel, Status.FAILED, provider, null, attempts, error);
    }
}
