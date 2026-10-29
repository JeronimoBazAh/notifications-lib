package com.example.notifications.provider;

import com.example.notifications.spi.TransportRequest;
import com.example.notifications.spi.TransportResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class JdkHttpTransportTest {

    @Test void sendsRequestAndReturnsStatusHeadersAndBody() throws Exception {
        AtomicReference<String> receivedBody = new AtomicReference<>();
        AtomicReference<String> receivedAuth = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/send", exchange -> {
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes()));
            receivedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.getResponseHeaders().add("X-Message-Id", "abc123");
            byte[] payload = "{\"ok\":true}".getBytes();
            exchange.sendResponseHeaders(202, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/send";
            TransportResponse res = new JdkHttpTransport().execute(
                    new TransportRequest("POST", url, Map.of("Authorization", "Bearer k", "Content-Type", "application/json"), "{\"a\":1}"));

            assertEquals(202, res.status());
            assertEquals("abc123", res.headers().get("x-message-id")); // case-insensitive lookup
            assertEquals("{\"ok\":true}", res.body());
            assertEquals("{\"a\":1}", receivedBody.get());
            assertEquals("Bearer k", receivedAuth.get());
        } finally {
            server.stop(0);
        }
    }

    @Test void connectionFailureSurfacesAsIOException() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        server.start();
        server.stop(0); // nothing is listening on this port anymore

        JdkHttpTransport transport = new JdkHttpTransport(Duration.ofSeconds(1), Duration.ofSeconds(1));
        assertThrows(IOException.class,
                () -> transport.execute(new TransportRequest("POST", "http://127.0.0.1:" + port + "/x", Map.of(), "{}")));
    }
}
