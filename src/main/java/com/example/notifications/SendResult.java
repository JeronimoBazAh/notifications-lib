package com.example.notifications;

import com.example.notifications.error.NotificationException;
import com.example.notifications.model.TipoCanal;

/**
 * Resultado de un envío. Los errores de entrega se devuelven como DATOS (nunca generados), lo que mantiene el proceso por lotes y asíncrono
 * fluye simple. Las personas que llaman y prefieren excepciones pueden llamar a {@link #orElseThrow()}.
 *
 * @param provider proveedor que entregó, o el último que intentó en caso de error (nulo si no hay ninguno)
 * @param providerMessageId ID del lado del proveedor en caso de éxito, nulo en caso de error
 * @param attempts el total de intentos realizados, entre proveedores
 * @param error nulo en caso de éxito
 */
public record SendResult(
        String notificationId, TipoCanal channel, Status status, String provider,
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

    static SendResult success(String id, TipoCanal channel, String provider, String messageId, int attempts) {
        return new SendResult(id, channel, Status.SENT, provider, messageId, attempts, null);
    }

    static SendResult failure(String id, TipoCanal channel, String provider, int attempts, NotificationException error) {
        return new SendResult(id, channel, Status.FAILED, provider, null, attempts, error);
    }
}
