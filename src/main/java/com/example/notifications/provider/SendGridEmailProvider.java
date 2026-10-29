package com.example.notifications.provider;

import com.example.notifications.Secret;
import com.example.notifications.error.SendException;
import com.example.notifications.model.TipoCanal;
import com.example.notifications.model.EmailNotificacion;
import com.example.notifications.spi.NotificationProvider;
import com.example.notifications.spi.ProviderResponse;
import com.example.notifications.spi.Transport;
import com.example.notifications.spi.TransportRequest;
import com.example.notifications.spi.TransportResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SendGrid v3 «Mail Send»: {@code POST /v3/mail/send}, clave de API (Bearer API key), cuerpo en formato JSON con
 * personalizations/from/subject/content. Una operación exitosa devuelve {@code 202 Accepted} con un cuerpo vacío y el
 *   identificador del mensaje en la cabecera {@code X-Message-Id}. Nota: SendGrid requiere que {@code text/plain} aparezca ANTES que {@code text/html}.
 */
public final class SendGridEmailProvider implements NotificationProvider<EmailNotificacion> {
    public static final String NAME = "sendgrid";
    private static final String URL = "https://api.sendgrid.com/v3/mail/send";

    private final Secret apiKey;
    private final Transport transport;

    public SendGridEmailProvider(Secret apiKey, Transport transport) {
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    /** Proveedor conectado a un transporte simulado: registra la solicitud y responde tal como lo haría SendGrid. */
    public static SendGridEmailProvider simulated(Secret apiKey) {
        return new SendGridEmailProvider(apiKey, SimulatedTransports.sendGrid());
    }

    @Override public String name() { return NAME; }
    @Override public TipoCanal channel() { return TipoCanal.EMAIL; }
    @Override public Class<EmailNotificacion> notificationType() { return EmailNotificacion.class; }

    @Override
    public ProviderResponse send(EmailNotificacion n) throws SendException {
        Map<String, Object> personalization = new LinkedHashMap<>();
        personalization.put("to", addresses(n.to()));
        if (!n.cc().isEmpty()) personalization.put("cc", addresses(n.cc()));
        if (!n.bcc().isEmpty()) personalization.put("bcc", addresses(n.bcc()));

        List<Map<String, String>> content = new ArrayList<>();
        if (n.textBody() != null && !n.textBody().isBlank()) content.add(Map.of("type", "text/plain", "value", n.textBody()));
        if (n.htmlBody() != null && !n.htmlBody().isBlank()) content.add(Map.of("type", "text/html", "value", n.htmlBody()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("personalizations", List.of(personalization));
        payload.put("from", Map.of("email", n.from()));
        payload.put("subject", n.subject());
        payload.put("content", content);

        TransportRequest request = new TransportRequest("POST", URL,
                Map.of("Authorization", "Bearer " + apiKey.reveal(), "Content-Type", "application/json"),
                Json.write(payload));

        TransportResponse response = ProviderSupport.execute(NAME, transport, request);
        return new ProviderResponse(response.headers().get("X-Message-Id"), "accepted");
    }

    private static List<Map<String, String>> addresses(List<String> emails) {
        return emails.stream().map(e -> Map.of("email", e)).toList();
    }
}
