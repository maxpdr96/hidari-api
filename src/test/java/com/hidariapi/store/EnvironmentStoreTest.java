package com.hidariapi.store;

import com.hidariapi.model.Environment;
import com.hidariapi.util.AppPaths;
import com.hidariapi.util.TestFs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentStoreTest {

    @BeforeEach
    void clean() throws Exception {
        TestFs.deleteRecursively(AppPaths.configDir());
    }

    @Test
    void saveGetListAndRemoveWork() {
        var store = new EnvironmentStore();
        var env = Environment.empty("dev").withVariable("base", "http://localhost");

        store.save(env);
        assertEquals(1, store.size());
        assertTrue(store.get("dev").isPresent());
        assertEquals("http://localhost", store.get("dev").orElseThrow().variables().get("base"));

        assertTrue(store.remove("dev"));
        assertEquals(0, store.size());
    }
}
