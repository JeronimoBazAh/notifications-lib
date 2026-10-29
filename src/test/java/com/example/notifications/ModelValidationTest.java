package com.example.notifications;

import com.example.notifications.error.ValidationException;
import com.example.notifications.model.TipoCanal;
import com.example.notifications.model.EmailNotificacion;
import com.example.notifications.model.PushNotificacion;
import com.example.notifications.model.SlackNotificacion;
import com.example.notifications.model.SmsNotificacion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelValidationTest {
    @Test
    void emailReportsAllViolationsAtOnce() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> EmailNotificacion.builder().from("nope").to("bad").subject(" ").build());
        assertEquals(4, e.violations().size()); // from, to, subject, body
    }

    @Test
    void validEmailBuilds() {
        EmailNotificacion n = EmailNotificacion.builder().from("a@b.com").to("c@d.io").htmlBody("<b>x</b>").subject("s").build();
        assertEquals(TipoCanal.EMAIL, n.channel());
        assertNotNull(n.id());
    }

    @Test
    void emailRequiresAtLeastOneRecipient() {
        assertThrows(ValidationException.class,
                () -> EmailNotificacion.builder().from("a@b.com").subject("s").textBody("t").build());
    }

    @Test
    void smsRequiresE164AndBoundedBody() {
        assertThrows(ValidationException.class, () -> SmsNotificacion.builder().to("3515551234").body("hi").build());
        assertThrows(ValidationException.class, () -> SmsNotificacion.builder().to("+5493515551234").body("x".repeat(1601)).build());
        assertDoesNotThrow(() -> SmsNotificacion.builder().to("+5493515551234").body("hi").build());
    }

    @Test
    void pushNeedsExactlyOneTarget() {
        assertThrows(ValidationException.class, () -> PushNotificacion.builder().title("t").build());
        assertThrows(ValidationException.class, () -> PushNotificacion.builder().deviceToken("x").topic("y").title("t").build());
        assertDoesNotThrow(() -> PushNotificacion.builder().topic("news").title("t").build());
    }

    @Test
    void slackRequiresTargetAndText() {
        assertThrows(ValidationException.class, () -> SlackNotificacion.builder().text("hi").build());
        assertThrows(ValidationException.class, () -> SlackNotificacion.builder().target("#a").build());
        assertDoesNotThrow(() -> SlackNotificacion.builder().target("#a").text("hi").build());
    }

    @Test
    void channelTypeIsNormalizedAndOpenToNewChannels() {
        assertEquals(TipoCanal.EMAIL, TipoCanal.of(" email "));
        assertEquals("WHATSAPP", TipoCanal.of("whatsapp").name());
        assertThrows(IllegalArgumentException.class, () -> TipoCanal.of(" "));
    }

    @Test
    void toStringDoesNotLeakPersonalData() {
        EmailNotificacion n = EmailNotificacion.builder().from("a@b.com").to("secret@d.com").subject("s").textBody("private").build();
        assertFalse(n.toString().contains("secret@d.com"));
        assertFalse(n.toString().contains("private"));
    }
}
