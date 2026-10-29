package com.example.notifications.spi;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Respuesta HTTP mínima. Los nombres de los encabezados no distinguen entre mayúsculas y minúsculas, tal como ocurre en HTTP (
 * en HTTP/2, aparecen en minúsculas).

 */
public record TransportResponse(int status, Map<String, String> headers, String body) {
    public TransportResponse {
        Map<String, String> caseInsensitive = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.putAll(headers);
        headers = Collections.unmodifiableMap(caseInsensitive);
        body = body == null ? "" : body;
    }
}
