package com.hidariapi.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonFormatterTest {

    private final JsonFormatter formatter = new JsonFormatter();

    @Test
    void prettifyFormatsValidJson() {
        var pretty = formatter.prettify("{\"a\":1}");
        assertTrue(pretty.contains("\n"));
        assertTrue(pretty.contains("\"a\""));
    }

    @Test
    void prettifyReturnsOriginalOnInvalidJson() {
        var invalid = "{a";
        assertEquals(invalid, formatter.prettify(invalid));
    }

    @Test
    void isValidJsonDetectsCorrectly() {
        assertTrue(formatter.isValidJson("{\"x\":1}"));
        assertFalse(formatter.isValidJson("x"));
        assertFalse(formatter.isValidJson(""));
    }
}
