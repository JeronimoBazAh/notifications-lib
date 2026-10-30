package com.example.notifications.model;

import java.time.Instant;
import java.util.Map;

/**
 * Contrato común para toda notificación. Cada canal define su propia implementación inmutable y autovalidante
 * (un correo electrónico tiene un asunto, un SMS no); por tanto, las diferencias entre canales se expresan a través del
 * sistema de tipos, en lugar de mediante un {@code Map<String, Object>} genérico.
 */
public interface Notification {
    /** Identificador único, útil para la correlación y la idempotencia por parte de quien realiza la llamada. */
    String id();

    ChanelType channel();

    Instant createdAt();

    /** Datos del llamante en formato libre (inquilino, ID de correlación...). Nunca se envían a los proveedores. */
    Map<String, String> metadata();
}
