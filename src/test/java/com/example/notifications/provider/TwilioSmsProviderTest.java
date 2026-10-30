package com.example.notifications.provider;

import com.example.notifications.Secret;
import com.example.notifications.model.SmsNotification;
import com.example.notifications.spi.NotificationProvider;
import com.example.notifications.spi.Transport;
import com.example.notifications.spi.TransportRequest;
import com.example.notifications.spi.TransportResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TwilioSmsProviderTest extends ProviderContractTest<SmsNotification> {
    @Override NotificationProvider<SmsNotification> provider(Transport t) { return new TwilioSmsProvider("ACx", Secret.of("tok"), "+15005550006", t); }
    @Override Transport happyTransport() { return SimulatedTransports.twilio(); }
    @Override
    SmsNotification sample() { return SmsNotification.builder().to("+5493515551234").body("hi").build(); }

    @Test void usesDefaultSenderWhenNotificationHasNone() throws Exception {
        AtomicReference<TransportRequest> seen = new AtomicReference<>();
        provider(r -> { seen.set(r); return new TransportResponse(201, Map.of(), "{\"sid\":\"SM1\",\"status\":\"queued\"}"); }).send(sample());
        assertEquals("https://api.twilio.com/2010-04-01/Accounts/ACx/Messages.json", seen.get().url());
        assertTrue(seen.get().body().contains("From=%2B15005550006"));
        assertTrue(seen.get().body().contains("To=%2B5493515551234"));
    }
}
