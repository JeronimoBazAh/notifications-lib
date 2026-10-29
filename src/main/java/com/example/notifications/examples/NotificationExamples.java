package com.example.notifications.examples;

import com.example.notifications.FailoverPolicy;
import com.example.notifications.NotificationService;
import com.example.notifications.RetryPolicy;
import com.example.notifications.Secret;
import com.example.notifications.SendResult;
import com.example.notifications.error.ValidationException;
import com.example.notifications.model.EmailNotification;
import com.example.notifications.model.Notification;
import com.example.notifications.model.PushNotification;
import com.example.notifications.model.SlackNotification;
import com.example.notifications.model.SmsNotification;
import com.example.notifications.provider.FcmPushProvider;
import com.example.notifications.provider.MailgunEmailProvider;
import com.example.notifications.provider.SendGridEmailProvider;
import com.example.notifications.provider.SlackChatProvider;
import com.example.notifications.provider.TwilioSmsProvider;
import com.example.notifications.spi.TransportResponse;
import com.example.notifications.template.MessageTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Runnable demo. Everything is simulated: no network, no real credentials. */
public final class NotificationExamples {

    public static void main(String[] args) {
        // In production read secrets from the environment: Secret.fromEnv("SENDGRID_API_KEY")
        try (NotificationService service = NotificationService.builder()
                .provider(SendGridEmailProvider.simulated(Secret.of("SG.demo")))
                .provider(MailgunEmailProvider.simulated(Secret.of("key-demo"), "mg.example.com"))
                .provider(TwilioSmsProvider.simulated("ACdemo", Secret.of("token-demo"), "+15005550006"))
                .provider(FcmPushProvider.simulated("demo-project", () -> Secret.of("ya29.demo")))
                .provider(SlackChatProvider.simulated(Secret.of("xoxb-demo")))
                .retryPolicy(RetryPolicy.exponential(3, Duration.ofMillis(200)).withJitter(0.2))
                .listener(e -> System.out.println("  event: " + e.type() + " " + e.channel() + " via " + e.provider()))
                .build()) {

            title("1) Same API for every channel");
            MessageTemplate welcome = MessageTemplate.of("Hi {{name}}, your code is {{code}}");
            Map<String, String> vars = Map.of("name", "Ana", "code", "4821");
            show(service.send(EmailNotification.builder().from("no-reply@acme.com").to("ana@example.com")
                    .subject("Welcome").textBody(welcome.render(vars))
                    .htmlBody(MessageTemplate.of("<b>Hi {{name}}</b>").renderHtml(vars)).build()));
            show(service.send(SmsNotification.builder().to("+5493515551234").body(welcome.render(vars)).build()));
            show(service.send(PushNotification.builder().deviceToken("fcm-token-123").title("Transfer received")
                    .body("You received $100").data("txId", "tx-42").build()));
            show(service.send(SlackNotification.builder().target("#alerts").text("Deploy finished").build()));

            title("2) Validation: every violation reported at once, before anything is sent");
            try {
                EmailNotification.builder().from("not-an-email").subject(" ").build();
            } catch (ValidationException e) {
                e.violations().forEach(v -> System.out.println("  - " + v));
            }

            title("3) Retry + failover: SendGrid is down (503), Mailgun takes over");
            try (NotificationService resilient = NotificationService.builder()
                    .provider(new SendGridEmailProvider(Secret.of("SG.demo"),
                            request -> new TransportResponse(503, Map.of(), "unavailable")))
                    .provider(MailgunEmailProvider.simulated(Secret.of("key-demo"), "mg.example.com"))
                    .retryPolicy(RetryPolicy.exponential(2, Duration.ofMillis(50)))
                    .failoverPolicy(FailoverPolicy.ALWAYS)
                    .listener(e -> System.out.println("  event: " + e.type() + " via " + e.provider()))
                    .build()) {
                show(resilient.send(EmailNotification.builder().from("no-reply@acme.com")
                        .to("ana@example.com").subject("Failover demo").textBody("hello").build()));
            }

            title("4) Async batch (virtual threads)");
            List<Notification> batch = List.of(
                    SmsNotification.builder().to("+5493515550001").body("Hi 1").build(),
                    SmsNotification.builder().to("+5493515550002").body("Hi 2").build(),
                    SmsNotification.builder().to("+5493515550003").body("Hi 3").build());
            service.sendAll(batch).join().forEach(NotificationExamples::show);
        }
    }

    private static void title(String text) {
        System.out.println("\n== " + text);
    }

    private static void show(SendResult r) {
        System.out.println(r.isSuccess()
                ? "OK   " + r.channel() + " via " + r.provider() + " id=" + r.providerMessageId() + " attempts=" + r.attempts()
                : "FAIL " + r.channel() + " " + r.error().code() + ": " + r.error().getMessage());
    }
}
