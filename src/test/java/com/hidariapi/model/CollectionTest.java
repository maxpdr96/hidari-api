package com.hidariapi.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CollectionTest {

    @Test
    void withRequestAndWithoutRequestWorks() {
        var req = ApiRequest.of("r1", HttpMethod.GET, "https://a");
        var col = Collection.empty("c1").withRequest(req);

        assertEquals(1, col.requests().size());
        var removed = col.withoutRequest(0);
        assertTrue(removed.requests().isEmpty());
        assertEquals(1, col.requests().size());
    }

    @Test
    void withoutRequestIgnoresInvalidIndex() {
        var req = ApiRequest.of("r1", HttpMethod.GET, "https://a");
        var col = Collection.empty("c1").withRequest(req);
        assertEquals(1, col.withoutRequest(7).requests().size());
    }
}
