package com.example.notifications;

import java.time.Duration;
import java.util.Objects;

/**
 * Exponential backoff applied PER PROVIDER and only to retryable failures.
 *
 * @param maxAttempts total attempts including the first one (1 = no retries)
 * @param jitter      0..1. Each delay is reduced by up to this fraction at random, so many clients that failed
 *                    together don't all retry at the same instant (thundering herd). 0 = deterministic.
 */
public record RetryPolicy(int maxAttempts, Duration initialDelay, double multiplier, Duration maxDelay, double jitter) {

    public RetryPolicy {
        Objects.requireNonNull(initialDelay, "initialDelay");
        Objects.requireNonNull(maxDelay, "maxDelay");
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        if (initialDelay.isNegative()) throw new IllegalArgumentException("initialDelay must be >= 0");
        if (multiplier < 1) throw new IllegalArgumentException("multiplier must be >= 1");
        if (maxDelay.compareTo(initialDelay) < 0) throw new IllegalArgumentException("maxDelay must be >= initialDelay");
        if (jitter < 0 || jitter > 1) throw new IllegalArgumentException("jitter must be between 0 and 1");
    }

    /** Deterministic policy (no jitter). */
    public RetryPolicy(int maxAttempts, Duration initialDelay, double multiplier, Duration maxDelay) {
        this(maxAttempts, initialDelay, multiplier, maxDelay, 0.0);
    }

    public static RetryPolicy none() {
        return new RetryPolicy(1, Duration.ZERO, 1, Duration.ZERO, 0.0);
    }

    /** Doubles the delay on each failure, capped at 30 seconds (or the initial delay if larger). */
    public static RetryPolicy exponential(int maxAttempts, Duration initialDelay) {
        Duration cap = initialDelay.compareTo(Duration.ofSeconds(30)) > 0 ? initialDelay : Duration.ofSeconds(30);
        return new RetryPolicy(maxAttempts, initialDelay, 2.0, cap, 0.0);
    }

    public RetryPolicy withJitter(double jitter) {
        return new RetryPolicy(maxAttempts, initialDelay, multiplier, maxDelay, jitter);
    }

    /** Base delay after the given failed attempt (1-based), without jitter. */
    public Duration delayAfter(int failedAttempt) {
        Duration d = Duration.ofMillis((long) (initialDelay.toMillis() * Math.pow(multiplier, failedAttempt - 1)));
        return d.compareTo(maxDelay) > 0 ? maxDelay : d;
    }

    /** Delay with jitter applied. {@code randomUnit} must be in [0, 1) (inject it to keep tests deterministic). */
    public Duration delayAfter(int failedAttempt, double randomUnit) {
        Duration base = delayAfter(failedAttempt);
        return Duration.ofMillis((long) (base.toMillis() * (1 - jitter * randomUnit)));
    }
}
