package com.example.notifications.provider;

import com.example.notifications.spi.Transport;
import com.example.notifications.spi.TransportRequest;
import com.example.notifications.spi.TransportResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Transporte HTTP real basado en el {@link HttpClient} del JDK (sin dependencias). Los tiempos de espera (*timeouts*) son obligatorios:
 * un proveedor que se quede bloqueado no debe provocar el bloqueo de quien realiza la llamada. Los tiempos de espera y los errores de E/S se manifiestan como {@link IOException}, los cuales
 * los proveedores clasifican como reintentables. Advertencia: reintentar una solicitud POST que agotó su tiempo de espera DESPUÉS de haber sido aceptada por el proveedor
 * puede duplicar un mensaje; utilice mecanismos de idempotencia en el lado del proveedor, siempre que estén disponibles.
 */
public final class JdkHttpTransport implements Transport {
    private final HttpClient client;
    private final Duration requestTimeout;

    public JdkHttpTransport() {
        this(Duration.ofSeconds(5), Duration.ofSeconds(10));
    }

    public JdkHttpTransport(Duration connectTimeout, Duration requestTimeout) {
        this.client = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        this.requestTimeout = requestTimeout;
    }

    @Override
    public TransportResponse execute(TransportRequest request) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(request.url())).timeout(requestTimeout);
        request.headers().forEach(builder::header);
        boolean hasBody = request.body() != null && !request.body().isEmpty();
        builder.method(request.method(), hasBody
                ? HttpRequest.BodyPublishers.ofString(request.body())
                : HttpRequest.BodyPublishers.noBody());
        try {
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            Map<String, String> headers = new HashMap<>();
            response.headers().map().forEach((name, values) -> {
                if (!values.isEmpty()) {
                    headers.put(name, values.get(0));
                }
            });
            return new TransportResponse(response.statusCode(), headers, response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while calling " + request.url(), e);
        }
    }
}
