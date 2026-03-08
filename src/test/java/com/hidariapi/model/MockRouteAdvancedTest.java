package com.hidariapi.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MockRouteAdvancedTest {

    @Test
    void supportsTimeoutAndScenarioCopies() {
        var route = MockRoute.json(HttpMethod.GET, "/state", "{}")
                .withTimeoutSeconds(2)
                .withScenarioStatusCodes(List.of(500, 200));

        assertEquals(2, route.timeoutSeconds());
        assertEquals(List.of(500, 200), route.scenarioStatusCodes());
    }
}
