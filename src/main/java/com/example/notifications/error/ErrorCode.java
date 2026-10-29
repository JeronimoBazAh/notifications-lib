package com.example.notifications.error;

/** Machine-readable reason of a failure. Lets callers branch without parsing messages. */
public enum ErrorCode {
    /** The notification itself is malformed (never reaches a provider). */
    VALIDATION,
    /** No provider is registered for the notification's channel. */
    NO_PROVIDER,
    /** Provider rejected our credentials (HTTP 401/403). Not retryable. */
    AUTHENTICATION,
    /** Provider throttled us (HTTP 429). Retryable. */
    RATE_LIMITED,
    /** Provider refused the request content (HTTP 4xx). Not retryable. */
    REJECTED_BY_PROVIDER,
    /** Provider down or network failure (HTTP 5xx / IO). Retryable. */
    PROVIDER_UNAVAILABLE,
    /** Anything we did not anticipate (bug in a provider, interruption...). */
    UNEXPECTED
}
