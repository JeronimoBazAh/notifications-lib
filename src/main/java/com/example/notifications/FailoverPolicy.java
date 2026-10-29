package com.example.notifications;

import com.example.notifications.error.ErrorCode;
import com.example.notifications.error.SendException;

/** Determina si un proveedor que ha fallado debe ceder el paso al siguiente proveedor registrado para el canal. */
public enum FailoverPolicy {
    /** Ante cualquier fallo, se pasa al siguiente proveedor. Maximiza la entrega. Opción predeterminada. */
    ALWAYS,
    /** No continúes si el proveedor ha rechazado el propio contenido (HTTP 4xx): es probable que otro proveedor también lo rechace. */
    SKIP_ON_REJECTION,
    /** Solo se utiliza el proveedor principal. */
    NEVER;

    boolean allows(SendException failure) {
        return switch (this) {
            case ALWAYS -> true;
            case NEVER -> false;
            case SKIP_ON_REJECTION -> failure.code() != ErrorCode.REJECTED_BY_PROVIDER;
        };
    }
}
