package com.example.notifications.spi;

import java.io.IOException;

/**
 * Seam between a provider and the network. Real implementation: wrap {@code java.net.http.HttpClient}.
 * Simulated / test implementation: a lambda. This is what makes providers testable without mocks or sockets.
 */
@FunctionalInterface
public interface Transport {
    TransportResponse execute(TransportRequest request) throws IOException;
}
