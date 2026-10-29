package com.example.notifications;

import com.example.notifications.event.NotificationListener;
import com.example.notifications.model.TipoCanal;
import com.example.notifications.model.Notificacion;
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
 * Interfaz pública de la biblioteca. Esta gestiona la configuración y la API (síncrona/asíncrona); el algoritmo de entrega
 * (enrutamiento, reintento, conmutación por error, eventos) reside en {@link DeliveryPipeline}.

 * Añadir un canal o un proveedor nunca requiere modificar esta clase (principio abierto/cerrado): simplemente se registran
 * en el {@link Builder}.
 */
public final class NotificationService implements AutoCloseable {
    private final DeliveryPipeline pipeline;
    private final Executor executor;
    private final ExecutorService ownedExecutor;

    private NotificationService(Builder b) {
        Map<TipoCanal, List<NotificationProvider<?>>> copy = new LinkedHashMap<>();
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

    /** Realiza el envío de forma síncrona. Nunca lanza excepciones ante problemas de entrega: examina el {@link SendResult} devuelto. */
    public SendResult send(Notificacion notificacion) {
        Objects.requireNonNull(notificacion, "notification");
        return pipeline.deliver(notificacion);
    }

    /** Envío no bloqueante. El objeto «future» se completa con normalidad incluso si la entrega falla (véase {@link SendResult#error()}). */
    public CompletableFuture<SendResult> sendAsync(Notificacion notificacion) {
        Objects.requireNonNull(notificacion, "notification");
        return CompletableFuture.supplyAsync(() -> pipeline.deliver(notificacion), executor);
    }

    /** Envía múltiples notificaciones de forma concurrente. Los resultados mantienen el orden de entrada; un fallo no afecta al resto. */
    public CompletableFuture<List<SendResult>> sendAll(Collection<? extends Notificacion> notifications) {
        List<CompletableFuture<SendResult>> futures = notifications.stream().map(this::sendAsync).toList();
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
    }

    /** Cierra el ejecutor interno (si quien realiza la llamada no proporcionó el suyo propio). */    @Override
    public void close() {
        if (ownedExecutor != null) {
            ownedExecutor.shutdown();
        }
    }

    public static final class Builder {
        private final Map<TipoCanal, List<NotificationProvider<?>>> providers = new LinkedHashMap<>();
        private final List<NotificationListener> listeners = new ArrayList<>();
        private RetryPolicy retryPolicy = RetryPolicy.none();
        private FailoverPolicy failoverPolicy = FailoverPolicy.ALWAYS;
        private Executor executor;
        private Sleeper sleeper = Sleeper.THREAD;

        /** El orden de registro determina la prioridad por canal: el primero es el principal; los demás, alternativas. */
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

        /** Ejecutor para envíos asíncronos. Quien realiza la llamada conserva la propiedad: el servicio no lo cerrará. */
        public Builder executor(Executor executor) {
            this.executor = Objects.requireNonNull(executor, "executor");
            return this;
        }

        /** Principalmente para pruebas: sustituye la pausa real entre reintentos. */
        public Builder sleeper(Sleeper sleeper) {
            this.sleeper = Objects.requireNonNull(sleeper, "sleeper");
            return this;
        }

        public NotificationService build() {
            return new NotificationService(this);
        }
    }
}
