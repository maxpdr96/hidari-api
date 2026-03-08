package com.hidariapi.store;

import com.hidariapi.model.ApiRequest;
import com.hidariapi.model.Collection;
import com.hidariapi.model.HttpMethod;
import com.hidariapi.util.AppPaths;
import com.hidariapi.util.TestFs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CollectionStoreTest {

    @BeforeEach
    void clean() throws Exception {
        TestFs.deleteRecursively(AppPaths.configDir());
    }

    @Test
    void saveGetByNameAndIndexAndRemoveWork() {
        var store = new CollectionStore();
        var col = Collection.empty("api").withRequest(ApiRequest.of("r1", HttpMethod.GET, "https://a"));

        store.save(col);
        assertEquals(1, store.size());
        assertTrue(store.get("api").isPresent());
        assertTrue(store.getByIndex(1).isPresent());
        assertFalse(store.getByIndex(2).isPresent());

        assertTrue(store.remove("api"));
        assertEquals(0, store.size());
    }
}
