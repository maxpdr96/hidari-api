package com.hidariapi.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpMethodTest {

    @Test
    void fromStringParsesCaseInsensitive() {
        assertEquals(HttpMethod.POST, HttpMethod.fromString("post"));
        assertEquals(HttpMethod.HEAD, HttpMethod.fromString("HEAD"));
    }

    @Test
    void fromStringFallsBackToGet() {
        assertEquals(HttpMethod.GET, HttpMethod.fromString("invalid"));
    }
}
