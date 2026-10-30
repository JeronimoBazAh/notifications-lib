package com.example.notifications.provider;

import com.example.notifications.Secret;
import com.example.notifications.error.SendException;
import com.example.notifications.model.ChanelType;
import com.example.notifications.model.PushNotification;
import com.example.notifications.spi.NotificationProvider;
import com.example.notifications.spi.ProviderResponse;
import com.example.notifications.spi.Transport;
import com.example.notifications.spi.TransportRequest;
import com.example.notifications.spi.TransportResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Firebase Cloud Messaging HTTP v1: {@code POST /v1/projects/{project}/messages:send}, token de acceso OAuth2 (tipo Bearer),
 *  * cuerpo JSON {@code {"message":{"token"|"topic", "notification":{...}, "data":{...}}}}.
 *  * Una operación exitosa devuelve un código {@code 200} junto con {@code {"name":"projects/{project}/messages/{id}"}}.
 *  * <p>
 *  * Los tokens de acceso de FCM tienen una vida útil corta (~1 h); por ello, el proveedor acepta un {@code Supplier<Secret>}
 *  * que el emisor implementa incluyendo su propia lógica de renovación de tokens (por ejemplo, mediante credenciales de cuenta de servicio de Google).
 */
public final class FcmPushProvider implements NotificationProvider<PushNotification> {
    public static final String NAME = "fcm";

    private final String projectId;
    private final Supplier<Secret> accessToken;
    private final Transport transport;

    public FcmPushProvider(String projectId, Supplier<Secret> accessToken, Transport transport) {
        this.projectId = Objects.requireNonNull(projectId, "projectId");
        this.accessToken = Objects.requireNonNull(accessToken, "accessToken");
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    public static FcmPushProvider simulated(String projectId, Supplier<Secret> accessToken) {
        return new FcmPushProvider(projectId, accessToken, SimulatedTransports.fcm(projectId));
    }

    @Override public String name() { return NAME; }
    @Override public ChanelType channel() { return ChanelType.PUSH; }
    @Override public Class<PushNotification> notificationType() { return PushNotification.class; }

    @Override
    public ProviderResponse send(PushNotification n) throws SendException {
        Map<String, Object> message = new LinkedHashMap<>();
        if (n.deviceToken() != null && !n.deviceToken().isBlank()) message.put("token", n.deviceToken());
        else message.put("topic", n.topic());

        Map<String, String> notification = new LinkedHashMap<>();
        if (n.title() != null) notification.put("title", n.title());
        if (n.body() != null) notification.put("body", n.body());
        message.put("notification", notification);
        if (!n.data().isEmpty()) message.put("data", n.data());

        TransportRequest request = new TransportRequest("POST",
                "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send",
                Map.of("Authorization", "Bearer " + accessToken.get().reveal(), "Content-Type", "application/json"),
                Json.write(Map.of("message", message)));

        TransportResponse response = ProviderSupport.execute(NAME, transport, request);
        return new ProviderResponse(ProviderSupport.jsonString(response.body(), "name"), "accepted");
    }
}
