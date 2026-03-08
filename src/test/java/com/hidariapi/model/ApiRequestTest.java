package com.hidariapi.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiRequestTest {

    @Test
    void withHeaderAndWithBodyAreImmutable() {
        var base = ApiRequest.of("n", HttpMethod.GET, "https://api.test");
        var withHeader = base.withHeader("A", "1");
        var withBody = withHeader.withBody("{}");

        assertTrue(base.headers().isEmpty());
        assertEquals("1", withHeader.headers().get("A"));
        assertEquals("{}", withBody.body());
    }

    @Test
    void toCurlContainsMethodHeadersAndEscapedBody() {
        var req = ApiRequest.of("n", HttpMethod.POST, "https://api.test")
                .withHeader("Content-Type", "application/json")
                .withBody("{'x':'y'}");

        var curl = req.toCurl();
        assertTrue(curl.contains("-X POST"));
        assertTrue(curl.contains("-H 'Content-Type: application/json'"));
        assertTrue(curl.contains("-d '"));
        assertTrue(curl.contains("https://api.test"));
    }
}
