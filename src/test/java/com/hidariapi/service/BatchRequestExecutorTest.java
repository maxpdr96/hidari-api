package com.hidariapi.service;

import com.hidariapi.model.ApiRequest;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class BatchRequestExecutorTest {

    private HttpServer server;
    private ExecutorService serverExecutor;
    private int port;

    @BeforeEach
    void setup() throws Exception {
        TestFs.deleteRecursively(AppPaths.configDir());
        server = HttpServer.create(new InetSocketAddress(0), 0);
        serverExecutor = Executors.newCachedThreadPool();
        server.setExecutor(serverExecutor);
        port = server.getAddress().getPort();
        server.createContext("/ok", ex -> {
            byte[] body = "{}".getBytes();
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.createContext("/slow", ex -> {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            byte[] body = "{}".getBytes();
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
        if (serverExecutor != null) serverExecutor.shutdownNow();
    }

    @Test
    void executesBatchAndAggregatesResults() {
        var api = new ApiService(new CollectionStore(), new HistoryStore(20), new EnvironmentStore(), 5);
        var exec = new BatchRequestExecutor(api);

        var result = exec.execute(ApiRequest.of("r", HttpMethod.GET, "http://localhost:" + port + "/ok"), 3);

        assertEquals(3, result.callCount());
        assertEquals(3, result.success());
        assertEquals(0, result.failed());
        assertEquals(3, result.calls().size());
        assertTrue(result.totalDurationMs() >= 0);
        assertTrue(result.averageDurationMs() >= 0);
    }

    @Test
    void executesBatchInParallelWhenParallelismIsGreaterThanOne() {
        var api = new ApiService(new CollectionStore(), new HistoryStore(20), new EnvironmentStore(), 5);
        var exec = new BatchRequestExecutor(api);

        var startedAt = System.currentTimeMillis();
        var result = exec.execute(ApiRequest.of("r", HttpMethod.GET, "http://localhost:" + port + "/slow"), 10, 5);
        var elapsedMs = System.currentTimeMillis() - startedAt;

        assertEquals(10, result.callCount());
        assertEquals(10, result.success());
        assertEquals(0, result.failed());
        assertEquals(10, result.calls().size());
        for (int i = 0; i < result.calls().size(); i++) {
            assertEquals(i + 1, result.calls().get(i).index());
        }
        assertTrue(elapsedMs < 2200, "parallel execution should be faster than fully sequential execution");
    }
}
