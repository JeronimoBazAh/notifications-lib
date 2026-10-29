package com.example.notifications.spi;

/**
 * Lo que indica un proveedor tras aceptar una notificación.
 *
 * @param messageId identificador en el lado del proveedor (SendGrid X-Message-Id, Twilio SID, nombre de mensaje de FCM, etc.)
 * @param status    estado en el lado del proveedor; por ejemplo, "en cola" ("queued")
 */
public record ProviderResponse(String messageId, String status) {
}
