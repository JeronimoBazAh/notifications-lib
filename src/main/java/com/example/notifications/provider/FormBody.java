package com.example.notifications.provider;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Builds an {@code application/x-www-form-urlencoded} body. Keys may repeat (Mailgun's multiple 'to'). */
public final class FormBody {
    private final List<String> pairs = new ArrayList<>();

    public FormBody add(String key, String value) {
        pairs.add(URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
        return this;
    }

    public FormBody addIfPresent(String key, String value) {
        return value == null || value.isBlank() ? this : add(key, value);
    }

    public String build() {
        return String.join("&", pairs);
    }
}
