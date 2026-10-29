package com.example.notifications.error;

/**
 * Delivery failed on the provider side (or on the way to it).
 * {@link #isRetryable()} tells the service whether trying again makes sense.
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

    /** Provider name, or {@code null} if no provider was involved. */
    public String provider() {
        return provider;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
