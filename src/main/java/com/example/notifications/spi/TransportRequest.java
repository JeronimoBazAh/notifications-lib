package com.example.notifications.spi;

import java.util.Map;

/** Modelo mínimo de solicitud HTTP. {@code toString} oculta las cabeceras, ya que estas contienen credenciales. */
public record TransportRequest(String method, String url, Map<String, String> headers, String body) {
    public TransportRequest {
        headers = Map.copyOf(headers);
    }

    @Override
    public String toString() {
        return "TransportRequest[" + method + " " + url + "]";
    }
}
