package com.example.notifications;

import com.example.notifications.error.ValidationException;
import com.example.notifications.model.ChannelType;
import com.example.notifications.model.EmailNotification;
import com.example.notifications.model.PushNotification;
import com.example.notifications.model.SlackNotification;
import com.example.notifications.model.SmsNotification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelValidationTest {
    @Test
    void emailReportsAllViolationsAtOnce() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> EmailNotification.builder().from("nope").to("bad").subject(" ").build());
        assertEquals(4, e.violations().size()); // from, to, subject, body
    }

    @Test
    void validEmailBuilds() {
        EmailNotification n = EmailNotification.builder().from("a@b.com").to("c@d.io").htmlBody("<b>x</b>").subject("s").build();
        assertEquals(ChannelType.EMAIL, n.channel());
        assertNotNull(n.id());
    }

    @Test
    void emailRequiresAtLeastOneRecipient() {
        assertThrows(ValidationException.class,
                () -> EmailNotification.builder().from("a@b.com").subject("s").textBody("t").build());
    }

    @Test
    void smsRequiresE164AndBoundedBody() {
        assertThrows(ValidationException.class, () -> SmsNotification.builder().to("3515551234").body("hi").build());
        assertThrows(ValidationException.class, () -> SmsNotification.builder().to("+5493515551234").body("x".repeat(1601)).build());
        assertDoesNotThrow(() -> SmsNotification.builder().to("+5493515551234").body("hi").build());
    }

    @Test
    void pushNeedsExactlyOneTarget() {
        assertThrows(ValidationException.class, () -> PushNotification.builder().title("t").build());
        assertThrows(ValidationException.class, () -> PushNotification.builder().deviceToken("x").topic("y").title("t").build());
        assertDoesNotThrow(() -> PushNotification.builder().topic("news").title("t").build());
    }

    @Test
    void slackRequiresTargetAndText() {
        assertThrows(ValidationException.class, () -> SlackNotification.builder().text("hi").build());
        assertThrows(ValidationException.class, () -> SlackNotification.builder().target("#a").build());
        assertDoesNotThrow(() -> SlackNotification.builder().target("#a").text("hi").build());
    }

    @Test
    void channelTypeIsNormalizedAndOpenToNewChannels() {
        assertEquals(ChannelType.EMAIL, ChannelType.of(" email "));
        assertEquals("WHATSAPP", ChannelType.of("whatsapp").name());
        assertThrows(IllegalArgumentException.class, () -> ChannelType.of(" "));
    }

    @Test
    void toStringDoesNotLeakPersonalData() {
        EmailNotification n = EmailNotification.builder().from("a@b.com").to("secret@d.com").subject("s").textBody("private").build();
        assertFalse(n.toString().contains("secret@d.com"));
        assertFalse(n.toString().contains("private"));
    }
}
