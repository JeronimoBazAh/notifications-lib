package com.example.notifications.model;

import com.example.notifications.error.ValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Recopila infracciones para que quien realiza la llamada obtenga todos los problemas de una sola vez. Clase auxiliar con visibilidad de paquete. */
final class Validation {
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9\\-]+(\\.[A-Za-z0-9\\-]+)*\\.[A-Za-z]{2,}$");
    /** E.164: '+', código de país (sin cero inicial), hasta 15 dígitos en total. Es el mismo formato que exige Twilio. */
    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    private final List<String> violations = new ArrayList<>();

    void require(boolean ok, String message) {
        if (!ok) {
            violations.add(message);
        }
    }

    void throwIfAny(String subject) {
        if (!violations.isEmpty()) {
            throw new ValidationException(subject, violations);
        }
    }

    static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    static boolean isEmail(String s) {
        return s != null && EMAIL.matcher(s).matches();
    }

    static boolean isE164(String s) {
        return s != null && E164.matcher(s).matches();
    }

    /** Copia inmutable segura frente a valores nulos; los elementos nulos se convierten en "" para que se reporten como no válidos, en lugar de provocar un NPE. */
    static List<String> copyOf(List<String> list) {
        return list == null ? List.of() : list.stream().map(s -> s == null ? "" : s).toList();
    }

    static Map<String, String> copyOf(Map<String, String> map) {
        return map == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }
}
