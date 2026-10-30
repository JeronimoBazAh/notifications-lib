package com.example.notifications.template;

import com.example.notifications.error.ValidationException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plantilla mínima {@code {{variable}}}. Si falta una variable, se produce una {@link ValidationException}
 * (nunca se renderiza silenciosamente como un valor vacío). Utilice {@link #renderHtml} para cuerpos HTML: los valores se escapan.


 */
public final class MensajeTemplate {
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.]+)\\s*}}");

    private final String source;

    private MensajeTemplate(String source) {
        this.source = source;
    }

    public static MensajeTemplate of(String source) {
        Objects.requireNonNull(source, "source");
        return new MensajeTemplate(source);
    }

    public Set<String> variables() {
        Set<String> names = new LinkedHashSet<>();
        Matcher m = VARIABLE.matcher(source);
        while (m.find()) {
            names.add(m.group(1));
        }
        return names;
    }

    /** Representación en texto plano (SMS, notificaciones push, parte de texto de un correo electrónico). */
    public String render(Map<String, String> values) {
        return replace(values, false);
    }

    /** Renderizado HTML: cada valor sustituido se somete a escape HTML para evitar inyecciones. */
    public String renderHtml(Map<String, String> values) {
        return replace(values, true);
    }

    private String replace(Map<String, String> values, boolean escapeHtml) {
        List<String> missing = variables().stream().filter(v -> values.get(v) == null).toList();
        if (!missing.isEmpty()) {
            throw new ValidationException("template", missing.stream().map(v -> "missing variable '" + v + "'").toList());
        }
        return VARIABLE.matcher(source).replaceAll(m -> {
            String value = values.get(m.group(1));
            return Matcher.quoteReplacement(escapeHtml ? escape(value) : value);
        });
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
