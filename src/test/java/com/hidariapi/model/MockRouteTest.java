package com.hidariapi.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MockRouteTest {

    @Test
    void routeBuildersAndCopiesWork() {
        var route = MockRoute.json(HttpMethod.GET, "/users", "[]")
                .withHeader("X-Test", "1")
                .withDelay(100)
                .withDescription("list users");

        assertEquals("GET /users", route.routeKey());
        assertEquals("1", route.headers().get("X-Test"));
        assertEquals(100, route.delay());
        assertEquals("list users", route.description());
    }

    @Test
    void matchesSupportsPathParamsAndIgnoresQuery() {
        var route = MockRoute.json(HttpMethod.GET, "/api/users/{id}", "{}");
        assertTrue(route.matches("GET", "/api/users/123?x=1"));
        assertFalse(route.matches("POST", "/api/users/123"));
        assertFalse(route.matches("GET", "/api/users"));
    }
}
