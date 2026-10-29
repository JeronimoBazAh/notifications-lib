package com.example.notifications;

import com.example.notifications.error.SendException;
import com.example.notifications.model.ChannelType;
import com.example.notifications.model.Notification;
import com.example.notifications.spi.NotificationProvider;
import com.example.notifications.spi.ProviderResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Proves Open/Closed: a brand-new channel is added from OUTSIDE the library, without modifying it. */
class ExtensibilityTest {
    static final ChannelType WHATSAPP = ChannelType.of("whatsapp");

    record WhatsAppNotification(String id, String to, String text) implements Notification {
        @Override public ChannelType channel() { return WHATSAPP; }
        @Override public Instant createdAt() { return Instant.EPOCH; }
        @Override public Map<String, String> metadata() { return Map.of(); }
    }

    static class WhatsAppProvider implements NotificationProvider<WhatsAppNotification> {
        @Override public String name() { return "whatsapp-cloud"; }
        @Override public ChannelType channel() { return WHATSAPP; }
        @Override public Class<WhatsAppNotification> notificationType() { return WhatsAppNotification.class; }
        @Override public ProviderResponse send(WhatsAppNotification n) throws SendException {
            return new ProviderResponse("wamid-123", "sent to " + n.to());
        }
    }

    @Test
    void newChannelWorksWithoutChangingTheLibrary() {
        try (NotificationService service = NotificationService.builder().provider(new WhatsAppProvider()).build()) {
            SendResult r = service.send(new WhatsAppNotification("1", "+5493515551234", "hola"));
            assertTrue(r.isSuccess());
            assertEquals(WHATSAPP, r.channel());
            assertEquals("whatsapp-cloud", r.provider());
        }
    }
}
