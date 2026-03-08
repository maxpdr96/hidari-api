package com.hidariapi.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentTest {

    @Test
    void withAndWithoutVariableWorks() {
        var env = Environment.empty("dev").withVariable("base", "http://localhost").withVariable("v", "1");
        assertEquals("http://localhost", env.variables().get("base"));

        var updated = env.withoutVariable("v");
        assertFalse(updated.variables().containsKey("v"));
        assertTrue(env.variables().containsKey("v"));
    }

    @Test
    void resolveReplacesPlaceholders() {
        var env = Environment.empty("dev").withVariable("host", "api.local").withVariable("id", "42");
        var resolved = env.resolve("https://{{host}}/users/{{id}}");
        assertEquals("https://api.local/users/42", resolved);
    }
}
