package com.hidariapi.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TemplateResolverTest {

    @Test
    void resolvesPlaceholdersAndCachesByExpression() {
        var cache = new HashMap<String, String>();
        var calls = new AtomicInteger();

        var out = TemplateResolver.resolve("{{name}} {{name}}", cache, expr -> {
            calls.incrementAndGet();
            return "john";
        });

        assertEquals("john john", out);
        assertEquals(1, calls.get());
    }

    @Test
    void keepsOriginalTokenWhenResolverReturnsNull() {
        var out = TemplateResolver.resolve("hello {{unknown}}", new HashMap<>(), expr -> null);
        assertEquals("hello {{unknown}}", out);
    }

    @Test
    void handlesNullInput() {
        assertNull(TemplateResolver.resolve(null, new HashMap<>(), expr -> "x"));
    }
}
