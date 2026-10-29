package com.example.notifications.error;

/** Motivo de un fallo, en un formato legible por máquina. Permite a quien realiza la llamada tomar distintas rutas sin necesidad de analizar mensajes. */
public enum ErrorCode {
    /** La notificación en sí está mal formada (nunca llega a un proveedor). */
    VALIDATION,
    /** No hay ningún proveedor registrado para el canal de la notificación. */
    NO_PROVIDER,
    /** El proveedor rechazó nuestras credenciales (HTTP 401/403). No admite reintento. */
    AUTHENTICATION,
    /** El proveedor nos ha limitado (HTTP 429). Reintentable. */
    RATE_LIMITED,
    /** El proveedor rechazó el contenido de la solicitud (HTTP 4xx). No admite reintento. */
    REJECTED_BY_PROVIDER,
    /** Proveedor caído o fallo de red (HTTP 5xx / IO). Reintentable. */
    PROVIDER_UNAVAILABLE,
    /** Cualquier cosa que no hayamos previsto (un error en un proveedor, una interrupción...). */
    UNEXPECTED
}
