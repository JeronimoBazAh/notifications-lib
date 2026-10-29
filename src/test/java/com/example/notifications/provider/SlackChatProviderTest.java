package com.example.notifications.provider;

import com.example.notifications.Secret;
import com.example.notifications.error.ErrorCode;
import com.example.notifications.error.SendException;
import com.example.notifications.model.SlackNotificacion;
import com.example.notifications.spi.NotificationProvider;
import com.example.notifications.spi.Transport;
import com.example.notifications.spi.TransportRequest;
import com.example.notifications.spi.TransportResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SlackChatProviderTest extends ProviderContractTest<SlackNotificacion> {
    @Override NotificationProvider<SlackNotificacion> provider(Transport t) { return new SlackChatProvider(Secret.of("xoxb-1"), t); }
    @Override Transport happyTransport() { return SimulatedTransports.slack(); }
    @Override
    SlackNotificacion sample() { return SlackNotificacion.builder().target("#alerts").text("deploy done").build(); }

    private SendException failWithBody(String body) {
        NotificationProvider<SlackNotificacion> p = provider(r -> new TransportResponse(200, Map.of(), body));
        return assertThrows(SendException.class, () -> p.send(sample()));
    }

    @Test void okFalseWithHttp200IsStillAFailure() {
        SendException e = failWithBody("{\"ok\":false,\"error\":\"channel_not_found\"}");
        assertEquals(ErrorCode.REJECTED_BY_PROVIDER, e.code());
        assertFalse(e.isRetryable());
    }

    @Test void invalidAuthIsAnAuthenticationError() {
        assertEquals(ErrorCode.AUTHENTICATION, failWithBody("{\"ok\":false,\"error\":\"invalid_auth\"}").code());
    }

    @Test void ratelimitedInBodyIsRetryable() {
        SendException e = failWithBody("{\"ok\":false,\"error\":\"ratelimited\"}");
        assertEquals(ErrorCode.RATE_LIMITED, e.code());
        assertTrue(e.isRetryable());
    }

    @Test void buildsRequestLikeSlackExpects() throws Exception {
        AtomicReference<TransportRequest> seen = new AtomicReference<>();
        provider(r -> { seen.set(r); return new TransportResponse(200, Map.of(), "{\"ok\":true,\"ts\":\"1.2\"}"); })
                .send(SlackNotificacion.builder().target("C123").text("hi").threadTs("1.1").build());
        assertEquals("https://slack.com/api/chat.postMessage", seen.get().url());
        assertEquals("Bearer xoxb-1", seen.get().headers().get("Authorization"));
        assertTrue(seen.get().body().contains("\"thread_ts\":\"1.1\""));
    }
}
