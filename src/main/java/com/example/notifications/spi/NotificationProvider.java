package com.example.notifications.spi;

import com.example.notifications.error.SendException;
import com.example.notifications.model.ChanelType;
import com.example.notifications.model.Notification;

/**
 * Interfaz del proveedor de servicios: lo ÚNICO que un nuevo proveedor (o un canal completamente nuevo) debe implementar.
 * <p>
 * Contrato: en caso de éxito, devuelve un {@link ProviderResponse}; ante cualquier fallo, lanza una {@link SendException},
 * indicando si es posible realizar un reintento. Las implementaciones deben ser seguras para su uso en entornos multihilo (thread-safe).
 *
 * @param <N> el tipo de notificación que este proveedor puede entregar


 */
public interface NotificationProvider<N extends Notification> {

    /** Nombre estable y legible por humanos, utilizado en resultados, eventos y registros (p. ej., "sendgrid"). */
    String name();

    ChanelType channel();

    /** Permite al servicio realizar el envío de forma tipocura, sin necesidad de conversiones de tipo no verificadas. */
    Class<N> notificationType();

    ProviderResponse send(N notification) throws SendException;
}
