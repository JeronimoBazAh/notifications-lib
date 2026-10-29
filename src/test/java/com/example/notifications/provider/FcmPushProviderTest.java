package com.example.notifications.provider;

import com.example.notifications.Secret;
import com.example.notifications.model.PushNotification;
import com.example.notifications.spi.NotificationProvider;
import com.example.notifications.spi.Transport;
import com.example.notifications.spi.TransportRequest;
import com.example.notifications.spi.TransportResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FcmPushProviderTest extends ProviderContractTest<PushNotification> {
    @Override NotificationProvider<PushNotification> provider(Transport t) { return new FcmPushProvider("proj", () -> Secret.of("ya29"), t); }
    @Override Transport happyTransport() { return SimulatedTransports.fcm("proj"); }
    @Override PushNotification sample() { return PushNotification.builder().deviceToken("tok").title("t").body("b").data("k", "v").build(); }

    @Test void buildsRequestLikeFcmV1Expects() throws Exception {
        AtomicReference<TransportRequest> seen = new AtomicReference<>();
        provider(r -> { seen.set(r); return new TransportResponse(200, Map.of(), "{\"name\":\"projects/proj/messages/1\"}"); }).send(sample());
        assertEquals("https://fcm.googleapis.com/v1/projects/proj/messages:send", seen.get().url());
        assertEquals("Bearer ya29", seen.get().headers().get("Authorization"));
        assertTrue(seen.get().body().contains("\"token\":\"tok\""));
        assertTrue(seen.get().body().contains("\"data\":{\"k\":\"v\"}"));
    }

    @Test void topicTargetUsesTopicField() throws Exception {
        AtomicReference<TransportRequest> seen = new AtomicReference<>();
        provider(r -> { seen.set(r); return new TransportResponse(200, Map.of(), "{\"name\":\"n\"}"); })
                .send(PushNotification.builder().topic("news").title("t").build());
        assertTrue(seen.get().body().contains("\"topic\":\"news\""));
        assertFalse(seen.get().body().contains("\"token\""));
    }

    @Test void accessTokenIsReadOnEveryCallSoRefreshedTokensAreUsed() throws Exception {
        int[] calls = {0};
        FcmPushProvider p = new FcmPushProvider("proj", () -> Secret.of("tok-" + (++calls[0])), happyTransport());
        p.send(sample());
        p.send(sample());
        assertEquals(2, calls[0]);
    }
}
