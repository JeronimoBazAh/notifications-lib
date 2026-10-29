package com.example.notifications.provider;

import com.example.notifications.error.ErrorCode;
import com.example.notifications.error.SendException;
import com.example.notifications.spi.Transport;
import com.example.notifications.spi.TransportRequest;
import com.example.notifications.spi.TransportResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *Componentes auxiliares compartidos para proveedores basados ​​en HTTP.
 * Centralizan la semántica de correspondencia entre estados HTTP y errores,
  de modo que todos los proveedores clasifiquen los fallos de la misma manera
 (y cualquier proveedor nuevo obtenga esta funcionalidad automáticamente).
 */
public final class ProviderSupport {
    private ProviderSupport() {
    }

    /** Ejecuta la solicitud y convierte cualquier resultado que no sea 2xx en una {@link SendException} clasificada. */

    public static TransportResponse execute(String provider, Transport transport, TransportRequest request)
            throws SendException {
        TransportResponse response;
        try {
            response = transport.execute(request);
        } catch (IOException e) {
            throw new SendException(ErrorCode.PROVIDER_UNAVAILABLE, provider, true,
                    provider + ": network error: " + e.getMessage(), e);
        }
        int status = response.status();
        if (status >= 200 && status < 300) {
            return response;
        }
        String detail = provider + ": HTTP " + status + " " + truncate(response.body(), 200);
        if (status == 401 || status == 403) {
            throw new SendException(ErrorCode.AUTHENTICATION, provider, false, detail);
        } else if (status == 429) {
            throw new SendException(ErrorCode.RATE_LIMITED, provider, true, detail);
        } else if (status >= 500) {
            throw new SendException(ErrorCode.PROVIDER_UNAVAILABLE, provider, true, detail);
        }
        throw new SendException(ErrorCode.REJECTED_BY_PROVIDER, provider, false, detail);
    }

    public static String basicAuth(String user, String password) {
        String raw = user + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** Extrae un campo de tipo cadena situado en un nivel superior (o casi) de un cuerpo JSON. Es suficiente para identificadores; para otros casos, utiliza un analizador (parser) adecuado. */    public static String jsonString(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    /** Es verdadero si el cuerpo JSON contiene {@code "key": true}. Necesario para Slack, que responde con un código HTTP 200 y {@code ok:false} en caso de error. */    public static boolean jsonTrue(String json, String key) {
        return Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*true").matcher(json).find();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
