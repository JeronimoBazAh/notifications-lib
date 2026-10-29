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

class SendGridEmailProviderTest extends ProviderContractTest<EmailNotification> {
    @Override NotificationProvider<EmailNotification> provider(Transport t) { return new SendGridEmailProvider(Secret.of("SG.k"), t); }
    @Override Transport happyTransport() { return SimulatedTransports.sendGrid(); }
    @Override EmailNotification sample() {
        return EmailNotification.builder().from("a@b.com").to("c@d.com").cc("e@f.com").subject("Hi").textBody("t").htmlBody("<b>h</b>").build();
    }

    @Test void buildsRequestLikeSendGridExpects() throws Exception {
        AtomicReference<TransportRequest> seen = new AtomicReference<>();
        provider(r -> { seen.set(r); return new TransportResponse(202, Map.of("X-Message-Id", "abc"), ""); }).send(sample());
        TransportRequest r = seen.get();
        assertEquals("https://api.sendgrid.com/v3/mail/send", r.url());
        assertEquals("Bearer SG.k", r.headers().get("Authorization"));
        assertTrue(r.body().contains("\"personalizations\""));
        assertTrue(r.body().contains("\"cc\""));
        assertTrue(r.body().indexOf("text/plain") < r.body().indexOf("text/html"));
    }
}
