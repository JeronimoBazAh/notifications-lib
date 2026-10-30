package com.example.notifications.error;

/**
 * La entrega fallo por parte del proveedor (o durante el trayecto hacia él).
 * {@link #isRetryable()} indica al servicio si tiene sentido realizar un nuevo intento.
 */
public final class SendException extends NotificationException {
    private static final long serialVersionUID = 1L;

    private final String provider;
    private final boolean retryable;

    public SendException(ErrorCode code, String provider, boolean retryable, String message, Throwable cause) {
        super(code, message, cause);
        this.provider = provider;
        this.retryable = retryable;
    }

    public SendException(ErrorCode code, String provider, boolean retryable, String message) {
        this(code, provider, retryable, message, null);
    }

    /** Nombre del proveedor, o {@code null} si no intervino ningún proveedor. */
    public String provider() {
        return provider;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
