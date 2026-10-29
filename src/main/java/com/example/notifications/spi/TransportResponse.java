package com.example.notifications.spi;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Minimal HTTP response. Header names are case-insensitive, as in HTTP (HTTP/2 lowercases them). */
public record TransportResponse(int status, Map<String, String> headers, String body) {
    public TransportResponse {
        Map<String, String> caseInsensitive = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.putAll(headers);
        headers = Collections.unmodifiableMap(caseInsensitive);
        body = body == null ? "" : body;
    }
}
