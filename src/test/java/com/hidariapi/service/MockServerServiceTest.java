package com.hidariapi.service;

import com.hidariapi.model.HttpMethod;
import com.hidariapi.model.MockRoute;
import com.hidariapi.store.MockStore;
import com.hidariapi.util.AppPaths;
import com.hidariapi.util.TestFs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class MockServerServiceTest {

    private MockServerService service;

    @BeforeEach
    void setup() throws Exception {
        TestFs.deleteRecursively(AppPaths.configDir());
        service = new MockServerService(new MockStore());
    }

    @AfterEach
    void tearDown() {
        if (service != null && service.isRunning()) {
            service.stop();
        }
    }

    @Test
    void servesTemplatedMockWithParamQueryAndFaker() throws Exception {
        int port;
        try (var socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        service.addRoute(MockRoute.json(HttpMethod.GET, "/users/{id}", "{\"id\":\"{{param.id}}\",\"q\":\"{{query.q}}\",\"cpf\":\"{{faker.cpf}}\"}"));
        service.start(port);

        var client = HttpClient.newHttpClient();
        var req = HttpRequest.newBuilder().uri(new URI("http://localhost:" + port + "/users/10?q=abc")).GET().build();
        var res = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("\"id\":\"10\""));
        assertTrue(res.body().contains("\"q\":\"abc\""));
        assertTrue(res.body().matches(".*\\\"cpf\\\":\\\"\\d{11}\\\".*"));
        assertFalse(service.getRequestLogs(10).isEmpty());
    }
}
