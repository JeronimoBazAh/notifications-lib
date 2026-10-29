package com.example.notifications.spi;

import java.io.IOException;

/**
 * Punto de conexión entre un proveedor y la red. Implementación real: envoltorio de {@code java.net.http.HttpClient}.
 * Implementación simulada o de prueba: una expresión lambda. Esto es lo que permite probar los proveedores sin necesidad de *mocks* ni *sockets*.


 */
@FunctionalInterface
public interface Transport {
    TransportResponse execute(TransportRequest request) throws IOException;
}
