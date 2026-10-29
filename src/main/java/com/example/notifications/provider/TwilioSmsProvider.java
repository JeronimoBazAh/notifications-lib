package com.example.notifications.provider;

import com.example.notifications.Secret;
import com.example.notifications.error.SendException;
import com.example.notifications.model.TipoCanal;
import com.example.notifications.model.SmsNotificacion;
import com.example.notifications.spi.NotificationProvider;
import com.example.notifications.spi.ProviderResponse;
import com.example.notifications.spi.Transport;
import com.example.notifications.spi.TransportRequest;
import com.example.notifications.spi.TransportResponse;

import java.util.Map;
import java.util.Objects;

/**
 * Twilio Programmable Messaging: {@code POST /2010-04-01/Accounts/{AccountSid}/Messages.json},
 * Autenticación básica ({@code AccountSid:AuthToken}), campos de formulario {@code To}, {@code From}, {@code Body}.
 * El éxito se indica mediante {@code 201 Created}, con un JSON que contiene {@code sid} y {@code status} ("queued").


 */
public final class TwilioSmsProvider implements NotificationProvider<SmsNotificacion> {
    public static final String NAME = "twilio";

    private final String accountSid;
    private final Secret authToken;
    private final String defaultFrom;
    private final Transport transport;

    /** @param defaultFrom remitente que se utiliza cuando la notificación no especifica ninguno */    public TwilioSmsProvider(String accountSid, Secret authToken, String defaultFrom, Transport transport) {
        this.accountSid = Objects.requireNonNull(accountSid, "accountSid");
        this.authToken = Objects.requireNonNull(authToken, "authToken");
        this.defaultFrom = Objects.requireNonNull(defaultFrom, "defaultFrom");
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    public static TwilioSmsProvider simulated(String accountSid, Secret authToken, String defaultFrom) {
        return new TwilioSmsProvider(accountSid, authToken, defaultFrom, SimulatedTransports.twilio());
    }

    @Override public String name() { return NAME; }
    @Override public TipoCanal channel() { return TipoCanal.SMS; }
    @Override public Class<SmsNotificacion> notificationType() { return SmsNotificacion.class; }

    @Override
    public ProviderResponse send(SmsNotificacion n) throws SendException {
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
