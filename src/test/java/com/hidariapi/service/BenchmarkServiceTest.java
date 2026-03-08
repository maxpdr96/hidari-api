package com.hidariapi.service;

import com.hidariapi.model.ApiRequest;
import com.hidariapi.model.HttpMethod;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BenchmarkServiceTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.createContext("/bench", ex -> {
            byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    void benchComputesRpsAndPercentiles() {
        var service = new BenchmarkService(5);
        var request = ApiRequest.of("bench", HttpMethod.GET, "http://localhost:" + port + "/bench");

        var result = service.run(request, 20, 5, 3);

        assertEquals(20, result.calls());
        assertEquals(0, result.failed());
        assertEquals(20, result.success());
        assertTrue(result.rps() > 0.0);
        assertTrue(result.p95() >= result.p50());
        assertTrue(result.p99() >= result.p95());
    }
}
