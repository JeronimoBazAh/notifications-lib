package com.example.notifications.provider;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonAndFormBodyTest {
    @Test void jsonEscapesQuotesBackslashesAndControlCharacters() {
        assertEquals("\"a\\\"b\\\\c\\n\\u0001\"", Json.write("a\"b\\c\n\u0001"));
    }

    @Test void jsonWritesNestedStructuresInInsertionOrder() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("n", 1);
        m.put("ok", true);
        m.put("list", List.of("x", "y"));
        m.put("nothing", null);
        assertEquals("{\"n\":1,\"ok\":true,\"list\":[\"x\",\"y\"],\"nothing\":null}", Json.write(m));
    }

    @Test void jsonRejectsUnsupportedTypes() {
        assertThrows(IllegalArgumentException.class, () -> Json.write(new Object()));
    }

    @Test void formBodyUrlEncodesAndAllowsRepeatedKeys() {
        String body = new FormBody().add("to", "a@b.com").add("to", "c@d.com").add("text", "hi there & bye").build();
        assertEquals("to=a%40b.com&to=c%40d.com&text=hi+there+%26+bye", body);
    }

    @Test void addIfPresentSkipsBlankValues() {
        assertEquals("a=1", new FormBody().add("a", "1").addIfPresent("b", " ").addIfPresent("c", null).build());
    }
}
