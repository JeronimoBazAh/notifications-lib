package com.example.notifications.error;

import java.util.Objects;

/** Raíz de todas las excepciones de la biblioteca. No verificada, de modo que se integra bien con lambdas y futuros. */
public class NotificationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    public NotificationException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
    }

    public ErrorCode code() {
        return code;
    }
}
