package com.hidariapi.store;

import com.hidariapi.model.HttpMethod;
import com.hidariapi.model.MockRoute;
import com.hidariapi.util.AppPaths;
import com.hidariapi.util.TestFs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MockStoreTest {

    @BeforeEach
    void clean() throws Exception {
        TestFs.deleteRecursively(AppPaths.configDir());
    }

    @Test
    void addFindUpdateAndRemoveWork() {
        var store = new MockStore();
        var route = MockRoute.json(HttpMethod.GET, "/users/{id}", "{}");

        store.add(route);
        assertEquals(1, store.size());
        assertTrue(store.findMatch("GET", "/users/1").isPresent());

        var updated = route.withDescription("desc");
        assertTrue(store.update(1, updated));
        assertEquals("desc", store.get(1).orElseThrow().description());

        assertTrue(store.remove(1));
        assertEquals(0, store.size());
    }
}
