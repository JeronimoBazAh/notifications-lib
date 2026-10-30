package com.example.notifications.provider;

import com.example.notifications.spi.Transport;
import com.example.notifications.spi.TransportRequest;
import com.example.notifications.spi.TransportResponse;

import java.util.Map;
import java.util.UUID;

/**
 * Fake transports that log the outgoing request (headers/credentials are NOT logged) and answer
 * exactly like the real API does on success. This is how the demo "sends" without touching the network.
 */
public final class SimulatedTransports {
    private static final System.Logger LOG = System.getLogger("notifications.simulated");

    private SimulatedTransports() {
    }

    public static Transport sendGrid() {
        return request -> {
            log(describe(request));
            return new TransportResponse(202, Map.of("X-Message-Id", UUID.randomUUID().toString().replace("-", "")), "");
        };
    }

    public static Transport mailgun(String domain) {
        return request -> {
            log(describe(request));
            return new TransportResponse(200, Map.of(),
                    "{\"id\":\"<" + UUID.randomUUID() + "@" + domain + ">\",\"message\":\"Queued. Thank you.\"}");
        };
    }

    public static Transport twilio() {
        return request -> {
            log(describe(request));
            return new TransportResponse(201, Map.of(),
                    "{\"sid\":\"SM" + UUID.randomUUID().toString().replace("-", "") + "\",\"status\":\"queued\"}");
        };
    }

    public static Transport fcm(String projectId) {
        return request -> {
            log(describe(request));
            return new TransportResponse(200, Map.of(),
                    "{\"name\":\"projects/" + projectId + "/messages/0:" + System.nanoTime() + "\"}");
        };
    }

    public static Transport slack() {
        return r -> {
            log(describe(r));
            return new TransportResponse(200, Map.of(),
                    "{\"ok\":true,\"channel\":\"C0123456\",\"ts\":\"" + (System.currentTimeMillis() / 1000) + ".000100\"}");
        };
    }

    /**
     * What is safe to log about a request: method and URL only. Headers carry credentials and the body carries
     * personal data (addresses, phone numbers, one-time codes), so neither is ever written to logs.
     */
    static String describe(TransportRequest request) {
        return request.method() + " " + request.url();
    }

    private static void log(String line) {
        LOG.log(System.Logger.Level.INFO, "[simulated] " + line);
    }
}
