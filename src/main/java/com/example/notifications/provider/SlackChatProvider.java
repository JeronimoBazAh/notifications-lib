package com.example.notifications.provider;

import com.example.notifications.Seguridad;
import com.example.notifications.error.ErrorCode;
import com.example.notifications.error.SendException;
import com.example.notifications.model.ChanelType;
import com.example.notifications.model.SlackNotification;
import com.example.notifications.spi.NotificationProvider;
import com.example.notifications.spi.ProviderResponse;
import com.example.notifications.spi.Transport;
import com.example.notifications.spi.TransportRequest;
import com.example.notifications.spi.TransportResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Slack Web API {@code chat.postMessage}: petición POST con un token de bot (tipo Bearer) y un cuerpo JSON {channel, text, thread_ts?}.
 * OJO: Slack devuelve un código HTTP 200 incluso en caso de error, con el formato {@code {"ok":false,"error":"channel_not_found"}}.
 * En caso de éxito, la respuesta es {@code {"ok":true,"channel":"C..","ts":"1700000000.000100"}}; el campo {@code ts} identifica el mensaje.
 */
public final class SlackChatProvider implements NotificationProvider<SlackNotification> {
    public static final String NAME = "slack";

    private final Seguridad botToken;
    private final Transport transport;

    public SlackChatProvider(Seguridad botToken, Transport transport) {
        this.botToken = Objects.requireNonNull(botToken, "botToken");
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    public static SlackChatProvider simulated(Seguridad botToken) {
        return new SlackChatProvider(botToken, SimulatedTransports.slack());
    }

    @Override public String name() { return NAME; }
    @Override public ChanelType channel() { return ChanelType.SLACK; }
    @Override public Class<SlackNotification> notificationType() { return SlackNotification.class; }

    @Override
    public ProviderResponse send(SlackNotification n) throws SendException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("channel", n.target());
        payload.put("text", n.text());
        if (n.threadTs() != null && !n.threadTs().isBlank()) {
            payload.put("thread_ts", n.threadTs());
        }

        TransportResponse res = ProviderSupport.execute(NAME, transport, new TransportRequest("POST",
                "https://slack.com/api/chat.postMessage",
                Map.of("Authorization", "Bearer " + botToken.reveal(), "Content-Type", "application/json; charset=utf-8"),
                Json.write(payload)));

        if (!ProviderSupport.jsonTrue(res.body(), "ok")) {
            throw toException(ProviderSupport.jsonString(res.body(), "error"));
        }
        return new ProviderResponse(ProviderSupport.jsonString(res.body(), "ts"), "posted");
    }

    private static SendException toException(String error) {
        String detail = NAME + ": " + error;
        return switch (error == null ? "" : error) {
            case "invalid_auth", "not_authed", "token_revoked", "token_expired", "account_inactive", "missing_scope" ->
                    new SendException(ErrorCode.AUTHENTICATION, NAME, false, detail);
            case "ratelimited", "rate_limited" ->
                    new SendException(ErrorCode.RATE_LIMITED, NAME, true, detail);
            case "service_unavailable", "internal_error", "fatal_error", "request_timeout" ->
                    new SendException(ErrorCode.PROVIDER_UNAVAILABLE, NAME, true, detail);
            default -> new SendException(ErrorCode.REJECTED_BY_PROVIDER, NAME, false, detail);
        };
    }
}
