package com.example.notifications;

import com.example.notifications.error.ErrorCode;
import com.example.notifications.error.SendException;
import com.example.notifications.event.EventoNotificacion;
import com.example.notifications.model.TipoCanal;
import com.example.notifications.model.Notificacion;
import com.example.notifications.spi.NotificationProvider;
import com.example.notifications.spi.ProviderResponse;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Algoritmo de entrega para UNA notificación: selecciona la cadena de proveedores de su canal, realiza reintentos con cada proveedor
 *  * conforme a la {@link RetryPolicy}, aplica mecanismos de conmutación por error (failover) según la {@link FailoverPolicy} y publica eventos.
 *  * No tiene conocimiento alguno sobre hilos o procesos asíncronos: eso es responsabilidad del servicio.
 */
final class DeliveryPipeline {
    private final Map<TipoCanal, List<NotificationProvider<?>>> providers;
    private final RetryPolicy retryPolicy;
    private final FailoverPolicy failoverPolicy;
    private final Sleeper sleeper;
    private final EventPublisher events;

    DeliveryPipeline(Map<TipoCanal, List<NotificationProvider<?>>> providers, RetryPolicy retryPolicy,
                     FailoverPolicy failoverPolicy, Sleeper sleeper, EventPublisher events) {
        this.providers = providers;
        this.retryPolicy = retryPolicy;
        this.failoverPolicy = failoverPolicy;
        this.sleeper = sleeper;
        this.events = events;
    }

    /** Nunca lanza excepciones: cada resultado se expresa como un {@link SendResult}. */
    SendResult deliver(Notificacion n) {
        try {
            return attemptDelivery(n);
        } catch (RuntimeException e) {
            return fail(n, null, 0,
                    new SendException(ErrorCode.UNEXPECTED, null, false, "Unexpected error: " + e.getMessage(), e));
        }
    }

    private SendResult attemptDelivery(Notificacion n) {
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
                    events.publish(EventoNotificacion.Type.SENT, n, provider.name(), attempts, null, response.messageId());
                    return new SendResult.Sent(n.id(), n.channel(), provider.name(), response.messageId(), attempts);
                } catch (SendException e) {
                    last = e;
                } catch (RuntimeException e) {
                    last = new SendException(ErrorCode.UNEXPECTED, provider.name(), false,
                            "Unexpected provider error: " + e.getMessage(), e);
                }
                events.publish(EventoNotificacion.Type.ATTEMPT_FAILED, n, provider.name(), attempts, last.code(), last.getMessage());

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
            events.publish(EventoNotificacion.Type.FAILOVER, n, provider.name(), attempts, last.code(), chain.get(p + 1).name());
        }
        return fail(n, last.provider(), attempts, last);
    }

    /** Realiza una conversión de captura del comodín, de modo que la conversión de tipo se verifique en tiempo de ejecución en lugar de ser una conversión no verificada. */
    private static <N extends Notificacion> ProviderResponse dispatch(NotificationProvider<N> provider, Notificacion n)
            throws SendException {
        if (!provider.notificationType().isInstance(n)) {
            throw new SendException(ErrorCode.UNEXPECTED, provider.name(), false,
                    "Provider '" + provider.name() + "' cannot deliver " + n.getClass().getSimpleName());
        }
        return provider.send(provider.notificationType().cast(n));
    }

    private SendResult fail(Notificacion n, String provider, int attempts, SendException error) {
        events.publish(EventoNotificacion.Type.FAILED, n, provider, attempts, error.code(), error.getMessage());
        return new SendResult.Failed(n.id(), n.channel(), provider, attempts, error);
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
