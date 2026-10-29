package com.example.notifications.spi;

import java.util.Map;

/** Minimal HTTP request model. {@code toString} hides headers because they carry credentials. */
public record TransportRequest(String method, String url, Map<String, String> headers, String body) {
    public TransportRequest {
        headers = Map.copyOf(headers);
    }

    @Override
    public String toString() {
        return "TransportRequest[" + method + " " + url + "]";
    }
}
