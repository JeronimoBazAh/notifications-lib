package com.example.notifications;

import com.example.notifications.error.ErrorCode;
import com.example.notifications.error.SendException;
import com.example.notifications.event.NotificationEvent;
import com.example.notifications.model.ChannelType;
import com.example.notifications.model.Notification;
import com.example.notifications.spi.NotificationProvider;
import com.example.notifications.spi.ProviderResponse;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Delivery algorithm for ONE notification: pick the provider chain of its channel, retry each provider
 * according to the {@link RetryPolicy}, fail over according to the {@link FailoverPolicy}, and publish events.
 * Knows nothing about threads or async: that is the service's concern.
 */
final class DeliveryPipeline {
    private final Map<ChannelType, List<NotificationProvider<?>>> providers;
    private final RetryPolicy retryPolicy;
    private final FailoverPolicy failoverPolicy;
    private final Sleeper sleeper;
    private final EventPublisher events;

    DeliveryPipeline(Map<ChannelType, List<NotificationProvider<?>>> providers, RetryPolicy retryPolicy,
                     FailoverPolicy failoverPolicy, Sleeper sleeper, EventPublisher events) {
        this.providers = providers;
        this.retryPolicy = retryPolicy;
        this.failoverPolicy = failoverPolicy;
        this.sleeper = sleeper;
        this.events = events;
    }

    /** Never throws: every outcome is expressed as a {@link SendResult}. */
    SendResult deliver(Notification n) {
        try {
            return attemptDelivery(n);
        } catch (RuntimeException e) {
            return fail(n, null, 0,
                    new SendException(ErrorCode.UNEXPECTED, null, false, "Unexpected error: " + e.getMessage(), e));
        }
    }

    private SendResult attemptDelivery(Notification n) {
        List<NotificationProvider<?>> chain = providers.getOrDefault(n.channel(), List.of());
        if (chain.isEmpty()) {
            return fail(n, null, 0, new SendException(ErrorCode.NO_PROVIDER, null, false,
                    "No provider registered for channel " + n.channel()));
        }

        SendException last = null;
        int attempts = 0;
        for (int p = 0; p < chain.size(); p++) {
            NotificationProvider<?> provider = chain.get(p);
            for (int attempt = 1; attempt <= retryPolicy.maxAttempts(); attempt++) {
                attempts++;
                try {
                    ProviderResponse response = dispatch(provider, n);
                    events.publish(NotificationEvent.Type.SENT, n, provider.name(), attempts, null, response.messageId());
                    return SendResult.success(n.id(), n.channel(), provider.name(), response.messageId(), attempts);
                } catch (SendException e) {
                    last = e;
                } catch (RuntimeException e) {
                    last = new SendException(ErrorCode.UNEXPECTED, provider.name(), false,
                            "Unexpected provider error: " + e.getMessage(), e);
                }
                events.publish(NotificationEvent.Type.ATTEMPT_FAILED, n, provider.name(), attempts, last.code(), last.getMessage());

                if (!last.isRetryable() || attempt == retryPolicy.maxAttempts()) {
                    break;
                }
                if (!pause(retryPolicy.delayAfter(attempt, ThreadLocalRandom.current().nextDouble()))) {
                    last = new SendException(ErrorCode.UNEXPECTED, provider.name(), false, "Interrupted while waiting to retry");
                    return fail(n, provider.name(), attempts, last);
                }
            }

            boolean hasNext = p < chain.size() - 1;
            if (!hasNext || !failoverPolicy.allows(last)) {
                break;
            }
            events.publish(NotificationEvent.Type.FAILOVER, n, provider.name(), attempts, last.code(), chain.get(p + 1).name());
        }
        return fail(n, last.provider(), attempts, last);
    }

    /** Capture-converts the wildcard so the cast is checked at runtime instead of being unchecked. */
    private static <N extends Notification> ProviderResponse dispatch(NotificationProvider<N> provider, Notification n)
            throws SendException {
        if (!provider.notificationType().isInstance(n)) {
            throw new SendException(ErrorCode.UNEXPECTED, provider.name(), false,
                    "Provider '" + provider.name() + "' cannot deliver " + n.getClass().getSimpleName());
        }
        return provider.send(provider.notificationType().cast(n));
    }

    private SendResult fail(Notification n, String provider, int attempts, SendException error) {
        events.publish(NotificationEvent.Type.FAILED, n, provider, attempts, error.code(), error.getMessage());
        return SendResult.failure(n.id(), n.channel(), provider, attempts, error);
    }

    private boolean pause(Duration delay) {
        try {
            sleeper.sleep(delay);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
