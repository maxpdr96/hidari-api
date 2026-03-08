package com.hidariapi.util;

import com.hidariapi.model.HttpMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CurlParserTest {

    private final CurlParser parser = new CurlParser();

    @Test
    void parsesMethodHeadersBodyAndUrl() {
        var req = parser.parse("curl -X PUT 'https://api.test/users/1' -H 'Content-Type: application/json' -d '{\"name\":\"A\"}'");

        assertEquals(HttpMethod.PUT, req.method());
        assertEquals("https://api.test/users/1", req.url());
        assertEquals("application/json", req.headers().get("Content-Type"));
        assertEquals("{\"name\":\"A\"}", req.body());
    }

    @Test
    void infersPostWhenBodyExists() {
        var req = parser.parse("curl 'https://api.test' --data-raw '{\"x\":1}'");
        assertEquals(HttpMethod.POST, req.method());
    }

    @Test
    void createsBasicAuthHeaderFromUserFlag() {
        var req = parser.parse("curl 'https://api.test' -u 'john:secret'");
        assertTrue(req.headers().get("Authorization").startsWith("Basic "));
    }

    @Test
    void throwsWhenUrlMissing() {
        var ex = assertThrows(IllegalArgumentException.class, () -> parser.parse("curl -X GET"));
        assertTrue(ex.getMessage().contains("URL"));
    }
}
