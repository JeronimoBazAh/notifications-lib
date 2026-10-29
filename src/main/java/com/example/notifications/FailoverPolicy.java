package com.example.notifications;

import com.example.notifications.error.ErrorCode;
import com.example.notifications.error.SendException;

/** Decides whether a failed provider should hand over to the next one registered for the channel. */
public enum FailoverPolicy {
    /** Any failure moves on to the next provider. Maximises delivery. Default. */
    ALWAYS,
    /** Do not move on when the provider rejected the content itself (HTTP 4xx): another provider will likely reject it too. */
    SKIP_ON_REJECTION,
    /** Only the primary provider is ever used. */
    NEVER;

    boolean allows(SendException failure) {
        return switch (this) {
            case ALWAYS -> true;
            case NEVER -> false;
            case SKIP_ON_REJECTION -> failure.code() != ErrorCode.REJECTED_BY_PROVIDER;
        };
    }
}
