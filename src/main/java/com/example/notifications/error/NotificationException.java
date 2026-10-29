package com.example.notifications.error;

import java.util.Objects;

/** Root of all library exceptions. Unchecked so it composes well with lambdas and futures. */
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
