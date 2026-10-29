package com.example.notifications.model;

import java.time.Instant;
import java.util.Map;

/**
 * Common contract of every notification. Each channel defines its own immutable, self-validating
 * implementation (an email has a subject, an SMS does not), so channel differences are expressed in the
 * type system instead of in a generic {@code Map<String, Object>}.
 */
public interface Notification {
    /** Unique id, useful for correlation and idempotency on the caller side. */
    String id();

    ChannelType channel();

    Instant createdAt();

    /** Free-form caller data (tenant, correlation id...). Never sent to providers. */
    Map<String, String> metadata();
}
