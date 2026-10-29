package com.example.notifications.provider;

import com.example.notifications.error.ErrorCode;
import com.example.notifications.error.SendException;
import com.example.notifications.model.Notificacion;
import com.example.notifications.spi.NotificationProvider;
import com.example.notifications.spi.ProviderResponse;
import com.example.notifications.spi.Transport;
import com.example.notifications.spi.TransportResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Contract every provider must satisfy. A new provider = one subclass of this. */
abstract class ProviderContractTest<N extends Notificacion> {
    abstract NotificationProvider<N> provider(Transport transport);
    abstract Transport happyTransport();
    abstract N sample();

    private SendException failWith(int status) {
        NotificationProvider<N> p = provider(r -> new TransportResponse(status, Map.of(), "error"));
        return assertThrows(SendException.class, () -> p.send(sample()));
    }

    @Test void happyPathReturnsProviderMessageId() throws Exception {
        ProviderResponse r = provider(happyTransport()).send(sample());
        assertNotNull(r.messageId());
        assertFalse(r.messageId().isBlank());
    }

    @Test void http401IsAuthenticationAndNotRetryable() {
        SendException e = failWith(401);
        assertEquals(ErrorCode.AUTHENTICATION, e.code());
        assertFalse(e.isRetryable());
    }

    @Test void http429IsRateLimitedAndRetryable() {
        SendException e = failWith(429);
        assertEquals(ErrorCode.RATE_LIMITED, e.code());
        assertTrue(e.isRetryable());
    }

    @Test void http503IsUnavailableAndRetryable() {
        SendException e = failWith(503);
        assertEquals(ErrorCode.PROVIDER_UNAVAILABLE, e.code());
        assertTrue(e.isRetryable());
    }

    @Test void http400IsRejectedAndNotRetryable() {
        SendException e = failWith(400);
        assertEquals(ErrorCode.REJECTED_BY_PROVIDER, e.code());
        assertFalse(e.isRetryable());
    }

    @Test void ioErrorIsRetryable() {
        NotificationProvider<N> p = provider(r -> { throw new IOException("timeout"); });
        SendException e = assertThrows(SendException.class, () -> p.send(sample()));
        assertTrue(e.isRetryable());
    }
}
