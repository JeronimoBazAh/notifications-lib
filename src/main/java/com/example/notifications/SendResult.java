package com.example.notifications;

import com.example.notifications.error.NotificationException;
import com.example.notifications.model.ChanelType;

import java.util.Objects;

/**
 * Resultado de un envío: {@link Sent} o {@link Failed}. Se modela mediante una jerarquía cerrada (*sealed hierarchy*), de modo que no es posible representar estados inválidos
 * (por ejemplo, un estado "sent" que contenga un error, o un estado "failed" sin él).
 * <p>
 * Los fallos en la entrega se devuelven como datos (nunca mediante excepciones), lo que simplifica los flujos por lotes (*batch*) y asíncronos. Quienes prefieran utilizar excepciones pueden llamar a {@link #orElseThrow()}; quienes prefieran la coincidencia de patrones (*pattern matching*) pueden evaluar ambos casos:
 * <pre>{@code
 * switch (result) {
 *     case SendResult.Sent s   -> log(s.providerMessageId());
 *     case SendResult.Failed f -> alert(f.error().code());
 * }
 * }
 */
public sealed interface SendResult permits SendResult.Sent, SendResult.Failed {

    String notificationId();

    ChanelType channel();

    /** Proveedor que realizó la entrega, o el último que se intentó utilizar en caso de fallo (null si no intervino ninguno). */
    String provider();

    /** Total de intentos realizados, entre todos los proveedores. */
    int attempts();

    /** ID del lado del proveedor en caso de éxito; {@code null} en caso de fallo. */
    String providerMessageId();

    /** El fallo, o {@code null} en caso de éxito. */
    NotificationException error();

    default boolean isSuccess() {
        return this instanceof Sent;
    }

    /** @return este resultado si la operación tuvo éxito; @throws NotificationException en caso contrario, la causa del fallo */
    SendResult orElseThrow();

    /** Entregado. {@code providerMessageId} puede ser nulo en el caso de proveedores que no devuelven ninguno. */
    record Sent(String notificationId, ChanelType channel, String provider, String providerMessageId, int attempts)
            implements SendResult {

        @Override
        public NotificationException error() {
            return null;
        }

        @Override
        public SendResult orElseThrow() {
            return this;
        }
    }

    /** No entregado. {@code error} siempre está presente. */
    record Failed(String notificationId, ChanelType channel, String provider, int attempts, NotificationException error)
            implements SendResult {

        public Failed {
            Objects.requireNonNull(error, "error");
        }

        @Override
        public String providerMessageId() {
            return null;
        }

        @Override
        public SendResult orElseThrow() {
            throw error;
        }
    }
}
