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
 * Real HTTP transport on top of the JDK's {@link HttpClient} (no dependencies). Timeouts are mandatory:
 * a provider that hangs must not hang the caller. Timeouts and I/O errors surface as {@link IOException}, which
 * the providers classify as retryable. Caveat: retrying a POST that timed out AFTER the provider accepted it
 * can duplicate a message; use provider-side idempotency features where they exist.
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
