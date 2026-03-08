package com.hidariapi.store;

import com.hidariapi.model.ApiRequest;
import com.hidariapi.model.HttpMethod;
import com.hidariapi.model.SavedRequest;
import com.hidariapi.util.AppPaths;
import com.hidariapi.util.TestFs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HistoryStoreTest {

    @BeforeEach
    void clean() throws Exception {
        TestFs.deleteRecursively(AppPaths.configDir());
    }

    @Test
    void addLastGetAndClearWork() {
        var store = new HistoryStore(3);
        var req = ApiRequest.of("r", HttpMethod.GET, "https://a");

        store.add(SavedRequest.error(req));
        store.add(SavedRequest.error(req));
        store.add(SavedRequest.error(req));
        store.add(SavedRequest.error(req));

        assertEquals(3, store.size());
        assertEquals(2, store.last(2).size());
        assertNotNull(store.get(1));
        assertNull(store.get(9));

        store.clear();
        assertEquals(0, store.size());
    }
}
