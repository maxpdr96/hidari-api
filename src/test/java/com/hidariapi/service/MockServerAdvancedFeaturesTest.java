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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MockServerAdvancedFeaturesTest {

    private MockServerService service;

    @BeforeEach
    void setup() throws Exception {
        TestFs.deleteRecursively(AppPaths.configDir());
        service = new MockServerService(new MockStore());
    }

    @AfterEach
    void tearDown() {
        if (service != null && service.isRunning()) service.stop();
    }

    @Test
    void timeoutConfigReturns408WhenDelayExceedsConfiguredLimit() throws Exception {
        int port;
        try (var socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        var route = new MockRoute(
                HttpMethod.GET,
                "/slow",
                200,
                Map.of("Content-Type", "application/json"),
                "{\"error\":\"timeout\"}",
                2000,
                1,
                List.of(),
                "slow with timeout"
        );
        service.addRoute(route);
        service.start(port);

        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var req = HttpRequest.newBuilder().uri(new URI("http://localhost:" + port + "/slow")).GET().build();

        var start = Instant.now();
        var res = client.send(req, HttpResponse.BodyHandlers.ofString());
        var elapsedMs = Duration.between(start, Instant.now()).toMillis();

        assertEquals(408, res.statusCode());
        assertTrue(elapsedMs >= 900);
    }

    @Test
    void delayAddsLatencyWithoutTimeoutWhenBelowLimit() throws Exception {
        int port;
        try (var socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        var route = new MockRoute(
                HttpMethod.GET,
                "/delayed",
                200,
                Map.of("Content-Type", "application/json"),
                "{\"ok\":true}",
                300,
                2,
                List.of(),
                "delayed route"
        );
        service.addRoute(route);
        service.start(port);

        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var req = HttpRequest.newBuilder().uri(new URI("http://localhost:" + port + "/delayed")).GET().build();

        var start = Instant.now();
        var res = client.send(req, HttpResponse.BodyHandlers.ofString());
        var elapsedMs = Duration.between(start, Instant.now()).toMillis();

        assertEquals(200, res.statusCode());
        assertTrue(elapsedMs >= 250);
    }

    @Test
    void scenarioStatusCodesFollowSequenceAndStickToLast() throws Exception {
        int port;
        try (var socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        var route = new MockRoute(
                HttpMethod.GET,
                "/state",
                200,
                Map.of("Content-Type", "application/json"),
                "{}",
                0,
                0,
                List.of(500, 200),
                "stateful"
        );
        service.addRoute(route);
        service.start(port);

        var client = HttpClient.newHttpClient();
        var uri = new URI("http://localhost:" + port + "/state");

        int s1 = client.send(HttpRequest.newBuilder().uri(uri).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode();
        int s2 = client.send(HttpRequest.newBuilder().uri(uri).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode();
        int s3 = client.send(HttpRequest.newBuilder().uri(uri).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode();

        assertEquals(500, s1);
        assertEquals(200, s2);
        assertEquals(200, s3);
    }
}
