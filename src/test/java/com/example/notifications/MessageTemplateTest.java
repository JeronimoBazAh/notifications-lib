package com.example.notifications;

import com.example.notifications.error.ValidationException;
import com.example.notifications.template.MessageTemplate;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MessageTemplateTest {
    @Test
    void rendersVariablesIgnoringWhitespaceInsideBraces() {
        assertEquals("Hi Ana, code 42", MessageTemplate.of("Hi {{ name }}, code {{code}}").render(Map.of("name", "Ana", "code", "42")));
    }

    @Test
    void listsVariables() {
        assertEquals(Set.of("a", "b"), MessageTemplate.of("{{a}} {{b}} {{a}}").variables());
    }

    @Test
    void missingVariablesAreAValidationErrorNotAnEmptyString() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> MessageTemplate.of("{{a}} {{b}}").render(Map.of("a", "x")));
        assertEquals(1, e.violations().size());
        assertTrue(e.violations().get(0).contains("'b'"));
    }

    @Test
    void htmlRenderingEscapesValues() {
        String html = MessageTemplate.of("<p>{{name}}</p>").renderHtml(Map.of("name", "<script>alert('x')</script>"));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void valuesWithRegexSpecialCharactersAreInsertedLiterally() {
        assertEquals("cost $1 \\ done", MessageTemplate.of("cost {{x}} done").render(Map.of("x", "$1 \\")));
    }
}
