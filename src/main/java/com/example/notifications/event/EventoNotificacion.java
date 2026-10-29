package com.example.notifications.event;

import com.example.notifications.error.ErrorCode;
import com.example.notifications.model.TipoCanal;

import java.time.Instant;
import java.util.Map;

/**
 * Evento de ciclo de vida publicado por el servicio (patrón observador / pub-sub).
 *
 * @param attempt  contador global de intentos para esta notificación (a través de los distintos proveedores); 0 si no se ha realizado ninguno
 * @param metadata metadatos de la notificación (inquilino, identificador de correlación, etc.) para que los suscriptores puedan correlacionar los eventos
 */
public record EventoNotificacion(
        Type type, String notificationId, TipoCanal channel, String provider, int attempt,
        ErrorCode errorCode, String detail, Map<String, String> metadata, Instant timestamp) {

    public enum Type {
        /** Entregado correctamente a un proveedor. detalle = ID de mensaje del proveedor. */
        SENT,
        /** Un intento falló (puede reintentarse). detail = mensaje de error. */
        ATTEMPT_FAILED,
        /** Se descarta un proveedor y se pasa al siguiente. detalle = nombre del siguiente proveedor. */
        FAILOVER,
        /** Todos los intentos en todos los proveedores fallaron. */
        FAILED
    }
}
