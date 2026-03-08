package com.hidariapi.service;

import com.hidariapi.model.MockRoute;
import com.hidariapi.store.MockStore;
import com.hidariapi.util.BrazilianDataGenerator;
import com.hidariapi.util.TemplateResolver;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
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
            var context = buildTemplateContext(exchange, route, path);

            // Delay simulado
            if (route.delay() > 0) {
                try { Thread.sleep(route.delay()); } catch (InterruptedException ignored) {}
            }

            // Headers
            for (var h : route.headers().entrySet()) {
                exchange.getResponseHeaders().add(h.getKey(), resolveTemplate(h.getValue(), context));
            }

            // Body
            var bodyText = resolveTemplate(route.body(), context);
            var body = bodyText != null ? bodyText.getBytes(StandardCharsets.UTF_8) : new byte[0];
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

    private TemplateContext buildTemplateContext(HttpExchange exchange, MockRoute route, String requestPath) {
        return new TemplateContext(
                extractPathParams(route.path(), requestPath),
                extractQueryParams(exchange),
                Instant.now(),
                System.currentTimeMillis(),
                new HashMap<>()
        );
    }

    private String resolveTemplate(String text, TemplateContext context) {
        return TemplateResolver.resolve(text, context.cache(), expr -> resolveTemplateExpression(expr, context));
    }

    private String resolveTemplateExpression(String expr, TemplateContext context) {
        return switch (expr) {
            case "$timestamp" -> String.valueOf(context.nowMillis());
            case "$isoTimestamp", "faker.timestamp" -> context.now().toString();
            case "$uuid", "faker.uuid" -> UUID.randomUUID().toString();
            case "$cpf", "faker.cpf" -> BrazilianDataGenerator.randomCpf();
            case "faker.int" -> String.valueOf(ThreadLocalRandom.current().nextInt(0, 10_000));
            case "faker.bool" -> String.valueOf(ThreadLocalRandom.current().nextBoolean());
            default -> resolveTemplateStructuredExpression(expr, context);
        };
    }

    private String resolveTemplateStructuredExpression(String expr, TemplateContext context) {
        if (expr.startsWith("param.")) {
            return context.pathParams().get(expr.substring("param.".length()));
        }
        if (expr.startsWith("query.")) {
            return context.queryParams().get(expr.substring("query.".length()));
        }
        if ("faker.word".equals(expr)) {
            var words = List.of("alpha", "beta", "gamma", "delta", "omega", "hidari");
            return words.get(ThreadLocalRandom.current().nextInt(words.size()));
        }
        return null;
    }

    private Map<String, String> extractPathParams(String routePath, String requestPath) {
        var params = new LinkedHashMap<String, String>();
        var routeParts = routePath.split("/");
        var reqParts = requestPath.split("\\?")[0].split("/");

        if (routeParts.length != reqParts.length) return params;

        for (int i = 0; i < routeParts.length; i++) {
            var part = routeParts[i];
            if (part.startsWith("{") && part.endsWith("}") && part.length() > 2) {
                var key = part.substring(1, part.length() - 1);
                params.put(key, urlDecode(reqParts[i]));
            }
        }
        return params;
    }

    private Map<String, String> extractQueryParams(HttpExchange exchange) {
        var params = new LinkedHashMap<String, String>();
        var rawQuery = exchange.getRequestURI().getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) return params;

        for (var pair : rawQuery.split("&")) {
            if (pair.isBlank()) continue;
            var idx = pair.indexOf('=');
            if (idx >= 0) {
                params.putIfAbsent(urlDecode(pair.substring(0, idx)), urlDecode(pair.substring(idx + 1)));
            } else {
                params.putIfAbsent(urlDecode(pair), "");
            }
        }
        return params;
    }

    private String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record TemplateContext(
            Map<String, String> pathParams,
            Map<String, String> queryParams,
            Instant now,
            long nowMillis,
            Map<String, String> cache
    ) {}
}
