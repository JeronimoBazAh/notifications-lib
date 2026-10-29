package com.example.notifications.provider;

import com.example.notifications.Secret;
import com.example.notifications.error.SendException;
import com.example.notifications.model.ChannelType;
import com.example.notifications.model.EmailNotification;
import com.example.notifications.spi.NotificationProvider;
import com.example.notifications.spi.ProviderResponse;
import com.example.notifications.spi.Transport;
import com.example.notifications.spi.TransportRequest;
import com.example.notifications.spi.TransportResponse;

import java.util.Map;
import java.util.Objects;

/**
 * Mailgun Messages API: {@code POST /v3/{domain}/messages}, HTTP Basic auth ({@code api:KEY}),
 * form-encoded body (repeated {@code to} fields). Success is {@code 200} with JSON
 * {@code {"id":"<...>","message":"Queued. Thank you."}}.
 */
public final class MailgunEmailProvider implements NotificationProvider<EmailNotification> {
    public static final String NAME = "mailgun";

    private final Secret apiKey;
    private final String domain;
    private final Transport transport;

    public MailgunEmailProvider(Secret apiKey, String domain, Transport transport) {
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.domain = Objects.requireNonNull(domain, "domain");
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    public static MailgunEmailProvider simulated(Secret apiKey, String domain) {
        return new MailgunEmailProvider(apiKey, domain, SimulatedTransports.mailgun(domain));
    }

    @Override public String name() { return NAME; }
    @Override public ChannelType channel() { return ChannelType.EMAIL; }
    @Override public Class<EmailNotification> notificationType() { return EmailNotification.class; }

    @Override
    public ProviderResponse send(EmailNotification n) throws SendException {
        FormBody form = new FormBody().add("from", n.from()).add("subject", n.subject());
        n.to().forEach(a -> form.add("to", a));
        n.cc().forEach(a -> form.add("cc", a));
        n.bcc().forEach(a -> form.add("bcc", a));
        form.addIfPresent("text", n.textBody()).addIfPresent("html", n.htmlBody());

        TransportRequest request = new TransportRequest("POST", "https://api.mailgun.net/v3/" + domain + "/messages",
                Map.of("Authorization", ProviderSupport.basicAuth("api", apiKey.reveal()),
                        "Content-Type", "application/x-www-form-urlencoded"),
                form.build());

        TransportResponse response = ProviderSupport.execute(NAME, transport, request);
        return new ProviderResponse(ProviderSupport.jsonString(response.body(), "id"), "queued");
    }
}
