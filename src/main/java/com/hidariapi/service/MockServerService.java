package com.hidariapi.service;

import com.hidariapi.model.MockRoute;
import com.hidariapi.store.MockStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Mock Server HTTP embutido. Responde requests conforme as rotas definidas no {@link MockStore}.
 * Usa o HttpServer built-in do Java (com.sun.net.httpserver).
 */
@Service
public class MockServerService {

    private static final Logger log = LoggerFactory.getLogger(MockServerService.class);

    private final MockStore store;
    private HttpServer server;
    private int port;
    private final List<RequestLog> requestLogs = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_LOGS = 100;

    /** Log de request recebido pelo mock server. */
    public record RequestLog(
            Instant timestamp,
            String method,
            String path,
            int statusCode,
            boolean matched
    ) {}

    public MockServerService(MockStore store) {
        this.store = store;
    }

    /** Inicia o mock server na porta especificada. */
    public void start(int port) throws IOException {
        if (server != null) {
            throw new IOException("Mock server ja esta rodando na porta " + this.port);
        }

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/", this::handleRequest);
        server.start();
        this.port = port;
        log.info("Mock server started on port {}", port);
    }

    /** Para o mock server. */
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            log.info("Mock server stopped");
        }
    }

    /** Verifica se esta rodando. */
    public boolean isRunning() {
        return server != null;
    }

    /** Retorna a porta atual. */
    public int getPort() {
        return port;
    }

    /** Retorna a base URL do mock server. */
    public String getBaseUrl() {
        return "http://localhost:" + port;
    }

    // ── Mock Store delegates ─────────────────────────────────────────────────

    public void addRoute(MockRoute route) {
        store.add(route);
    }

    public boolean removeRoute(int index) {
        return store.remove(index);
    }

    public boolean updateRoute(int index, MockRoute route) {
        return store.update(index, route);
    }

    public List<MockRoute> listRoutes() {
        return store.list();
    }

    public java.util.Optional<MockRoute> getRoute(int index) {
        return store.get(index);
    }

    public void clearRoutes() {
        store.clear();
    }

    public int routeCount() {
        return store.size();
    }

    // ── Request logs ─────────────────────────────────────────────────────────

    /** Retorna os ultimos N logs de requests. */
    public List<RequestLog> getRequestLogs(int limit) {
        var logs = new ArrayList<>(requestLogs);
        Collections.reverse(logs);
        return logs.stream().limit(limit).toList();
    }

    /** Limpa os logs. */
    public void clearLogs() {
        requestLogs.clear();
    }

    // ── Handler ──────────────────────────────────────────────────────────────

    private void handleRequest(HttpExchange exchange) throws IOException {
        var method = exchange.getRequestMethod();
        var path = exchange.getRequestURI().getPath();

        // CORS preflight
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        var match = store.findMatch(method, path);

        if (match.isPresent()) {
            var route = match.get();

            // Delay simulado
            if (route.delay() > 0) {
                try { Thread.sleep(route.delay()); } catch (InterruptedException ignored) {}
            }

            // Headers
            for (var h : route.headers().entrySet()) {
                exchange.getResponseHeaders().add(h.getKey(), h.getValue());
            }

            // Body
            var body = route.body() != null ? route.body().getBytes(StandardCharsets.UTF_8) : new byte[0];
            exchange.sendResponseHeaders(route.statusCode(), body.length > 0 ? body.length : -1);
            if (body.length > 0) {
                exchange.getResponseBody().write(body);
            }

            logRequest(method, path, route.statusCode(), true);
            log.debug("{} {} -> {} (matched: {})", method, path, route.statusCode(), route.description());
        } else {
            // Nenhuma rota encontrada
            var notFound = """
                    {"error": "No mock route found", "method": "%s", "path": "%s"}""".formatted(method, path);
            var bytes = notFound.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(404, bytes.length);
            exchange.getResponseBody().write(bytes);

            logRequest(method, path, 404, false);
            log.debug("{} {} -> 404 (no matching route)", method, path);
        }

        exchange.close();
    }

    private void logRequest(String method, String path, int status, boolean matched) {
        requestLogs.add(new RequestLog(Instant.now(), method, path, status, matched));
        while (requestLogs.size() > MAX_LOGS) {
            requestLogs.removeFirst();
        }
    }
}
