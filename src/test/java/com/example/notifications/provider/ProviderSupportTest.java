package com.example.notifications.provider;

import com.example.notifications.error.ErrorCode;
import com.example.notifications.error.SendException;
import com.example.notifications.spi.TransportRequest;
import com.example.notifications.spi.TransportResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProviderSupportTest {
    private static final TransportRequest REQUEST = new TransportRequest("POST", "http://x", Map.of(), "");

    @Test void basicAuthEncodesUserAndPassword() {
        assertEquals("Basic dXNlcjpwYXNz", ProviderSupport.basicAuth("user", "pass"));
    }

    @Test void jsonStringExtractsEscapedValues() {
        assertEquals("a\\\"b", ProviderSupport.jsonString("{\"id\":\"a\\\"b\"}", "id"));
        assertNull(ProviderSupport.jsonString("{\"other\":\"x\"}", "id"));
    }

    @Test void jsonTrueOnlyMatchesBooleanTrue() {
        assertTrue(ProviderSupport.jsonTrue("{\"ok\": true}", "ok"));
        assertFalse(ProviderSupport.jsonTrue("{\"ok\":false}", "ok"));
        assertFalse(ProviderSupport.jsonTrue("{\"ok\":\"true\"}", "ok"));
    }

    @Test void successfulStatusIsReturnedUntouched() throws Exception {
        TransportResponse res = ProviderSupport.execute("p", r -> new TransportResponse(201, Map.of(), "{}"), REQUEST);
        assertEquals(201, res.status());
    }

    @Test void longErrorBodiesAreTruncatedInTheMessage() {
        SendException e = assertThrows(SendException.class,
                () -> ProviderSupport.execute("p", r -> new TransportResponse(400, Map.of(), "x".repeat(1000)), REQUEST));
        assertTrue(e.getMessage().length() < 300);
        assertEquals(ErrorCode.REJECTED_BY_PROVIDER, e.code());
    }

    @Test void networkErrorsBecomeRetryableUnavailable() {
        SendException e = assertThrows(SendException.class,
                () -> ProviderSupport.execute("p", r -> { throw new IOException("reset"); }, REQUEST));
        assertEquals(ErrorCode.PROVIDER_UNAVAILABLE, e.code());
        assertTrue(e.isRetryable());
    }

    @Test void responseHeadersAreCaseInsensitive() {
        TransportResponse res = new TransportResponse(202, Map.of("x-message-id", "abc"), "");
        assertEquals("abc", res.headers().get("X-Message-Id"));
    }
}
