package com.hidariapi.service;

import com.hidariapi.store.CollectionStore;
import com.hidariapi.store.EnvironmentStore;
import com.hidariapi.store.HistoryStore;
import com.hidariapi.store.MockStore;
import com.hidariapi.util.AppPaths;
import com.hidariapi.util.TestFs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiImportServiceTest {

    private ApiService apiService;
    private MockServerService mockServerService;
    private ApiImportService importService;

    @BeforeEach
    void setup() throws Exception {
        TestFs.deleteRecursively(AppPaths.configDir());
        apiService = new ApiService(new CollectionStore(), new HistoryStore(20), new EnvironmentStore(), 5);
        mockServerService = new MockServerService(new MockStore());
        importService = new ApiImportService(apiService, mockServerService);
    }

    @Test
    void importsOpenApiToCollectionAndMocks() throws Exception {
        String openApi = """
                {
                  "openapi":"3.0.0",
                  "info":{"title":"Pet API"},
                  "servers":[{"url":"https://api.pet.local"}],
                  "paths":{
                    "/pets":{
                      "get":{"operationId":"listPets","responses":{"200":{"description":"ok","content":{"application/json":{"example":{"items":[]}}}}}},
                      "post":{"operationId":"createPet","requestBody":{"content":{"application/json":{"example":{"name":"rex"}}}},"responses":{"201":{"description":"created"}}}
                    }
                  }
                }
                """;

        var result = importService.importOpenApi(openApi, "pets-import", null, true);

        assertEquals("pets-import", result.collectionName());
        assertEquals(2, result.importedRequests());
        assertEquals(2, result.createdMocks());
        assertEquals(2, mockServerService.routeCount());
        var collection = apiService.getCollection("pets-import");
        assertTrue(collection.isPresent());
        assertEquals(2, collection.get().requests().size());
    }

    @Test
    void importsPostmanCollection() throws Exception {
        String postman = """
                {
                  "info":{"name":"Demo"},
                  "item":[
                    {
                      "name":"List users",
                      "request":{
                        "method":"GET",
                        "url":{"raw":"https://api.demo/users"}
                      }
                    },
                    {
                      "name":"Create user",
                      "request":{
                        "method":"POST",
                        "url":"https://api.demo/users",
                        "header":[{"key":"Content-Type","value":"application/json"}],
                        "body":{"mode":"raw","raw":"{\\"name\\":\\"ana\\"}"}
                      }
                    }
                  ]
                }
                """;

        var result = importService.importPostman(postman, "postman-import");
        assertEquals("postman-import", result.collectionName());
        assertEquals(2, result.importedRequests());
        var collection = apiService.getCollection("postman-import");
        assertTrue(collection.isPresent());
        assertEquals(2, collection.get().requests().size());
    }
}
