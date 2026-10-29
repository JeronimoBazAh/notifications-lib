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
 * Minimal {@code {{variable}}} template. A missing variable is a {@link ValidationException}
 * (never silently rendered as empty). Use {@link #renderHtml} for HTML bodies: values are escaped.
 */
public final class MessageTemplate {
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.]+)\\s*}}");

    private final String source;

    private MessageTemplate(String source) {
        this.source = source;
    }

    public static MessageTemplate of(String source) {
        Objects.requireNonNull(source, "source");
        return new MessageTemplate(source);
    }

    public Set<String> variables() {
        Set<String> names = new LinkedHashSet<>();
        Matcher m = VARIABLE.matcher(source);
        while (m.find()) {
            names.add(m.group(1));
        }
        return names;
    }

    /** Plain-text rendering (SMS, push, email text part). */
    public String render(Map<String, String> values) {
        return replace(values, false);
    }

    /** HTML rendering: every substituted value is HTML-escaped to prevent injection. */
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
