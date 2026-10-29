package com.example.notifications.spi;

/**
 * What a provider says after accepting a notification.
 *
 * @param messageId provider-side identifier (SendGrid X-Message-Id, Twilio SID, FCM message name...)
 * @param status    provider-side status, e.g. "queued"
 */
public record ProviderResponse(String messageId, String status) {
}
