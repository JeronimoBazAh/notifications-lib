package com.example.notifications;

import com.example.notifications.error.ValidationException;
import com.example.notifications.template.MensajeTemplate;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MensajeTemplateTest {
    @Test
    void rendersVariablesIgnoringWhitespaceInsideBraces() {
        assertEquals("Hola Ana, code 42", MensajeTemplate.of("Hola {{ name }}, code {{code}}").render(Map.of("name", "Ana", "code", "42")));
    }

    @Test
    void listsVariables() {
        assertEquals(Set.of("a", "b"), MensajeTemplate.of("{{a}} {{b}} {{a}}").variables());
    }

    @Test
    void missingVariablesAreAValidationErrorNotAnEmptyString() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> MensajeTemplate.of("{{a}} {{b}}").render(Map.of("a", "x")));
        assertEquals(1, e.violations().size());
        assertTrue(e.violations().get(0).contains("'b'"));
    }

    @Test
    void htmlRenderingEscapesValues() {
        String html = MensajeTemplate.of("<p>{{name}}</p>").renderHtml(Map.of("name", "<script>alert('x')</script>"));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void valuesWithRegexSpecialCharactersAreInsertedLiterally() {
        assertEquals("costo $1 \\ done", MensajeTemplate.of("costo {{x}} done").render(Map.of("x", "$1 \\")));
    }
}
