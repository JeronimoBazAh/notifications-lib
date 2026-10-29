package com.example.notifications;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RetryJitterTest {
    @Test
    void jitterReducesDelayWithinTheConfiguredFraction() {
        RetryPolicy p = RetryPolicy.exponential(3, Duration.ofSeconds(10)).withJitter(0.5);
        assertEquals(Duration.ofSeconds(10), p.delayAfter(1, 0.0));
        assertEquals(Duration.ofSeconds(5), p.delayAfter(1, 1.0));
    }

    @Test
    void zeroJitterIsDeterministic() {
        RetryPolicy p = RetryPolicy.exponential(3, Duration.ofSeconds(10));
        assertEquals(p.delayAfter(2), p.delayAfter(2, 0.99));
    }

    @Test
    void jitterOutOfRangeIsRejected() {
        RetryPolicy p = RetryPolicy.none();
        assertThrows(IllegalArgumentException.class, () -> p.withJitter(1.5));
        assertThrows(IllegalArgumentException.class, () -> p.withJitter(-0.1));
    }
}
