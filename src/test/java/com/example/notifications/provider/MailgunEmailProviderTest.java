package com.example.notifications.provider;

import com.example.notifications.Secret;
import com.example.notifications.model.EmailNotification;
import com.example.notifications.spi.NotificationProvider;
import com.example.notifications.spi.Transport;
import com.example.notifications.spi.TransportRequest;
import com.example.notifications.spi.TransportResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MailgunEmailProviderTest extends ProviderContractTest<EmailNotification> {
    @Override NotificationProvider<EmailNotification> provider(Transport t) { return new MailgunEmailProvider(Secret.of("k"), "mg.example.com", t); }
    @Override Transport happyTransport() { return SimulatedTransports.mailgun("mg.example.com"); }
    @Override EmailNotification sample() {
        return EmailNotification.builder().from("a@b.com").to("c@d.com").to("x@y.com").subject("Hi").textBody("t").build();
    }

    @Test void buildsRequestLikeMailgunExpects() throws Exception {
        AtomicReference<TransportRequest> seen = new AtomicReference<>();
        provider(r -> { seen.set(r); return new TransportResponse(200, Map.of(), "{\"id\":\"<1@mg>\"}"); }).send(sample());
        assertEquals("https://api.mailgun.net/v3/mg.example.com/messages", seen.get().url());
        assertTrue(seen.get().headers().get("Authorization").startsWith("Basic "));
        assertEquals(2, seen.get().body().split("to=", -1).length - 1); // repeated 'to' field
    }
}
