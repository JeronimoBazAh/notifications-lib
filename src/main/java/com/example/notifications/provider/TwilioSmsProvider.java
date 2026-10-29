package com.example.notifications.provider;

import com.example.notifications.Secret;
import com.example.notifications.error.SendException;
import com.example.notifications.model.ChannelType;
import com.example.notifications.model.SmsNotification;
import com.example.notifications.spi.NotificationProvider;
import com.example.notifications.spi.ProviderResponse;
import com.example.notifications.spi.Transport;
import com.example.notifications.spi.TransportRequest;
import com.example.notifications.spi.TransportResponse;

import java.util.Map;
import java.util.Objects;

/**
 * Twilio Programmable Messaging: {@code POST /2010-04-01/Accounts/{AccountSid}/Messages.json},
 * Basic auth ({@code AccountSid:AuthToken}), form fields {@code To}, {@code From}, {@code Body}.
 * Success is {@code 201 Created} with JSON containing {@code sid} and {@code status} ("queued").
 */
public final class TwilioSmsProvider implements NotificationProvider<SmsNotification> {
    public static final String NAME = "twilio";

    private final String accountSid;
    private final Secret authToken;
    private final String defaultFrom;
    private final Transport transport;

    /** @param defaultFrom sender used when the notification does not specify one */
    public TwilioSmsProvider(String accountSid, Secret authToken, String defaultFrom, Transport transport) {
        this.accountSid = Objects.requireNonNull(accountSid, "accountSid");
        this.authToken = Objects.requireNonNull(authToken, "authToken");
        this.defaultFrom = Objects.requireNonNull(defaultFrom, "defaultFrom");
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    public static TwilioSmsProvider simulated(String accountSid, Secret authToken, String defaultFrom) {
        return new TwilioSmsProvider(accountSid, authToken, defaultFrom, SimulatedTransports.twilio());
    }

    @Override public String name() { return NAME; }
    @Override public ChannelType channel() { return ChannelType.SMS; }
    @Override public Class<SmsNotification> notificationType() { return SmsNotification.class; }

    @Override
    public ProviderResponse send(SmsNotification n) throws SendException {
        String body = new FormBody()
                .add("To", n.to())
                .add("From", n.from() != null ? n.from() : defaultFrom)
                .add("Body", n.body())
                .build();

        TransportRequest request = new TransportRequest("POST",
                "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json",
                Map.of("Authorization", ProviderSupport.basicAuth(accountSid, authToken.reveal()),
                        "Content-Type", "application/x-www-form-urlencoded"),
                body);

        TransportResponse response = ProviderSupport.execute(NAME, transport, request);
        return new ProviderResponse(ProviderSupport.jsonString(response.body(), "sid"),
                ProviderSupport.jsonString(response.body(), "status"));
    }
}
