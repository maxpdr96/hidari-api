package com.hidariapi.service;

import com.hidariapi.model.ApiRequest;
import com.hidariapi.model.Environment;
import com.hidariapi.model.HttpMethod;
import com.hidariapi.store.CollectionStore;
import com.hidariapi.store.EnvironmentStore;
import com.hidariapi.store.HistoryStore;
import com.hidariapi.util.AppPaths;
import com.hidariapi.util.TestFs;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ApiServiceTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void setup() throws Exception {
        TestFs.deleteRecursively(AppPaths.configDir());

        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.createContext("/seed", ex -> {
            byte[] bytes = "{\"nested\":{\"id\":\"7\"}}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        server.createContext("/item/7", ex -> {
            byte[] bytes = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    void resolvesEnvironmentRuntimeAndLastResponseTemplates() throws Exception {
        var service = new ApiService(new CollectionStore(), new HistoryStore(20), new EnvironmentStore(), 5);
        var base = "http://localhost:" + port;
        service.saveEnvironment(Environment.empty("dev").withVariable("base_url", base));
        service.setActiveEnvironment("dev");

        var first = service.execute(ApiRequest.of("seed", HttpMethod.GET, "{{base_url}}/seed"));
        assertEquals(200, first.statusCode());

        var secondReq = ApiRequest.of("second", HttpMethod.GET, "{{base_url}}/item/{{last.body.nested.id}}")
                .withHeader("X-Prev-Status", "{{last.status}}")
                .withHeader("X-Cpf", "{{$cpf}}");

        var second = service.execute(secondReq);
        assertEquals(200, second.statusCode());

        var sent = service.getLastRequest();
        assertEquals(base + "/item/7", sent.url());
        assertEquals("200", sent.headers().get("X-Prev-Status"));
        assertTrue(sent.headers().get("X-Cpf").matches("\\d{11}"));
    }

    @Test
    void saveResponseToFileWritesLastBody() throws Exception {
        var client = HttpClient.newHttpClient();
        var req = HttpRequest.newBuilder().uri(new java.net.URI("http://localhost:" + port + "/seed")).build();
        var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());

        var service = new ApiService(new CollectionStore(), new HistoryStore(20), new EnvironmentStore(), 5);
        service.execute(ApiRequest.of("seed", HttpMethod.GET, "http://localhost:" + port + "/seed"));

        Path out = Path.of("target/test-home/out/resp.json");
        service.saveResponseToFile(out.toString());
        assertTrue(Files.exists(out));
        assertTrue(Files.readString(out).contains("nested"));
    }
}
