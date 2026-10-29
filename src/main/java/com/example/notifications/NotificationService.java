package com.example.notifications;

import com.example.notifications.event.NotificationListener;
import com.example.notifications.model.ChannelType;
import com.example.notifications.model.Notification;
import com.example.notifications.spi.NotificationProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Public facade of the library. It owns configuration and the sync/async API; the delivery algorithm
 * (routing, retry, failover, events) lives in {@link DeliveryPipeline}.
 * <p>
 * Adding a channel or a provider never requires changing this class (Open/Closed): they are just registered
 * in the {@link Builder}.
 */
public final class NotificationService implements AutoCloseable {
    private final DeliveryPipeline pipeline;
    private final Executor executor;
    private final ExecutorService ownedExecutor;

    private NotificationService(Builder b) {
        Map<ChannelType, List<NotificationProvider<?>>> copy = new LinkedHashMap<>();
        b.providers.forEach((channel, list) -> copy.put(channel, List.copyOf(list)));
        this.pipeline = new DeliveryPipeline(Map.copyOf(copy), b.retryPolicy, b.failoverPolicy, b.sleeper,
                new EventPublisher(b.listeners));
        if (b.executor != null) {
            this.executor = b.executor;
            this.ownedExecutor = null;
        } else {
            // Sending is blocking I/O: virtual threads (Java 21) are the natural fit.
            this.ownedExecutor = Executors.newVirtualThreadPerTaskExecutor();
            this.executor = ownedExecutor;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Sends synchronously. Never throws for delivery problems: inspect the returned {@link SendResult}. */
    public SendResult send(Notification notification) {
        Objects.requireNonNull(notification, "notification");
        return pipeline.deliver(notification);
    }

    /** Non-blocking send. The future completes normally even if delivery fails (see {@link SendResult#error()}). */
    public CompletableFuture<SendResult> sendAsync(Notification notification) {
        Objects.requireNonNull(notification, "notification");
        return CompletableFuture.supplyAsync(() -> pipeline.deliver(notification), executor);
    }

    /** Sends many notifications concurrently. Results keep input order; one failure does not affect the rest. */
    public CompletableFuture<List<SendResult>> sendAll(Collection<? extends Notification> notifications) {
        List<CompletableFuture<SendResult>> futures = notifications.stream().map(this::sendAsync).toList();
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
    }

    /** Shuts down the internal executor (if the caller did not supply their own). */
    @Override
    public void close() {
        if (ownedExecutor != null) {
            ownedExecutor.shutdown();
        }
    }

    public static final class Builder {
        private final Map<ChannelType, List<NotificationProvider<?>>> providers = new LinkedHashMap<>();
        private final List<NotificationListener> listeners = new ArrayList<>();
        private RetryPolicy retryPolicy = RetryPolicy.none();
        private FailoverPolicy failoverPolicy = FailoverPolicy.ALWAYS;
        private Executor executor;
        private Sleeper sleeper = Sleeper.THREAD;

        /** Registration order is priority order per channel: first = primary, the rest = fallbacks. */
        public Builder provider(NotificationProvider<?> provider) {
            Objects.requireNonNull(provider, "provider");
            providers.computeIfAbsent(provider.channel(), c -> new ArrayList<>()).add(provider);
            return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy");
            return this;
        }

        public Builder failoverPolicy(FailoverPolicy failoverPolicy) {
            this.failoverPolicy = Objects.requireNonNull(failoverPolicy, "failoverPolicy");
            return this;
        }

        public Builder listener(NotificationListener listener) {
            this.listeners.add(Objects.requireNonNull(listener, "listener"));
            return this;
        }

        /** Executor for async sends. The caller keeps ownership: the service will not shut it down. */
        public Builder executor(Executor executor) {
            this.executor = Objects.requireNonNull(executor, "executor");
            return this;
        }

        /** Mainly for tests: replaces real sleeping between retries. */
        public Builder sleeper(Sleeper sleeper) {
            this.sleeper = Objects.requireNonNull(sleeper, "sleeper");
            return this;
        }

        public NotificationService build() {
            return new NotificationService(this);
        }
    }
}
