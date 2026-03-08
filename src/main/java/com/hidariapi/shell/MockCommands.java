package com.hidariapi.shell;

import com.hidariapi.model.HttpMethod;
import com.hidariapi.model.MockRoute;
import com.hidariapi.service.ApiService;
import com.hidariapi.service.LanguageService;
import com.hidariapi.service.MockServerService;
import com.hidariapi.util.JsonFormatter;
import org.springframework.stereotype.Component;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

/**
 * Mock Server commands — creates fake APIs directly in the terminal.
 */
@Component
public class MockCommands extends LocalizedSupport {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final MockServerService mockService;
    private final ApiService apiService;
    private final JsonFormatter jsonFormatter;

    public MockCommands(MockServerService mockService, ApiService apiService, JsonFormatter jsonFormatter, LanguageService lang) {
        super(lang);
        this.mockService = mockService;
        this.apiService = apiService;
        this.jsonFormatter = jsonFormatter;
    }

    // ========== SERVER CONTROL =============================================

    @Command(name = "mock-start", description = "Inicia o mock server")
    public String mockStart(
            @Option(longName = "port", defaultValue = "8089", description = "Porta do servidor") int port) {
        try {
            mockService.start(port);
            var sb = new StringBuilder();
            sb.append(styled(BOLD_GREEN, t("Mock server iniciado!\n\n", "Mock server started!\n\n")));
            sb.append(styled(DIM, "  Base URL: "));
            sb.append(styled(CYAN, mockService.getBaseUrl())).append("\n");
            sb.append(styled(DIM, "  " + t("Rotas:    ", "Routes:   ")));
            sb.append(styled(CYAN, String.valueOf(mockService.routeCount()))).append("\n\n");
            sb.append(styled(DIM, "  " + t("Use 'mock-add' para criar rotas e 'mock-logs' para ver requests recebidos.\n", "Use 'mock-add' to create routes and 'mock-logs' to see received requests.\n")));
            return sb.toString();
        } catch (IOException e) {
            return styled(RED, t("Erro ao iniciar: ", "Error starting: ") + e.getMessage());
        }
    }

    @Command(name = "mock-stop", description = "Para o mock server")
    public String mockStop() {
        if (!mockService.isRunning()) {
            return styled(YELLOW, t("Mock server nao esta rodando.", "Mock server is not running."));
        }
        mockService.stop();
        return styled(GREEN, t("Mock server parado.", "Mock server stopped."));
    }

    @Command(name = "mock-status", description = "Mostra status do mock server")
    public String mockStatus() {
        if (!mockService.isRunning()) {
            return styled(DIM, t("Mock server nao esta rodando. Use 'mock-start' para iniciar.", "Mock server is not running. Use 'mock-start' to start."));
        }
        var sb = new StringBuilder();
        sb.append(styled(BOLD_GREEN, "\n  " + t("Mock Server - Ativo", "Mock Server - Active") + "\n\n"));
        sb.append(styled(DIM, "  Base URL: ")).append(styled(CYAN, mockService.getBaseUrl())).append("\n");
        sb.append(styled(DIM, "  " + t("Porta:    ", "Port:     "))).append(styled(CYAN, String.valueOf(mockService.getPort()))).append("\n");
        sb.append(styled(DIM, "  " + t("Rotas:    ", "Routes:   "))).append(styled(CYAN, String.valueOf(mockService.routeCount()))).append("\n");
        return sb.toString();
    }

    // ========== ROUTE MANAGEMENT ===========================================

    @Command(name = "mock-add", description = "Adiciona uma rota ao mock server")
    public String mockAdd(
            @Option(description = "Metodo HTTP (GET, POST, PUT, DELETE, etc.)", required = true) String method,
            @Option(description = "Path da rota (ex: /api/users, /api/users/{id})") String path,
            @Option(longName = "status", defaultValue = "200", description = "Status code da resposta") int status,
            @Option(longName = "body", defaultValue = "",
                    description = "Body da resposta (use @arquivo.json para ler de arquivo)") String body,
            @Option(longName = "header", defaultValue = "",
                    description = "Headers (formato Key:Value, multiplos separados por ;)") String headers,
            @Option(longName = "delay", defaultValue = "0", description = "Delay em ms antes de responder") long delay,
            @Option(longName = "timeout-config", defaultValue = "0", description = "Limite de timeout em segundos (retorna 408 se exceder)") long timeoutSeconds,
            @Option(longName = "scenario", defaultValue = "", description = "Sequencia stateful de status (ex: 500,500,200)") String scenario,
            @Option(longName = "desc", defaultValue = "", description = "Descricao da rota") String desc) {

        var resolvedBody = resolveBody(body);
        var route = new MockRoute(
                HttpMethod.fromString(method), path, status,
                parseHeaders(headers), resolvedBody, delay, timeoutSeconds, parseScenarioStatuses(scenario), desc);

        mockService.addRoute(route);

        var sb = new StringBuilder();
        sb.append(styled(GREEN, t("Rota adicionada: ", "Route added: ")));
        sb.append(styled(BOLD, route.method() + " " + route.path()));
        sb.append(styled(DIM, " -> " + route.statusCode()));
        if (mockService.isRunning()) {
            sb.append(styled(DIM, "\n  " + t("Teste: ", "Test: ")));
            sb.append(styled(CYAN, mockService.getBaseUrl() + path));
        }
        return sb.toString();
    }

    @Command(name = "mock-add-json", description = "Adiciona rota que retorna JSON (atalho)")
    public String mockAddJson(
            @Option(description = "Metodo HTTP", required = true) String method,
            @Option(description = "Path da rota") String path,
            @Option(longName = "body", description = "Body JSON (use @arquivo.json para ler de arquivo)") String body,
            @Option(longName = "status", defaultValue = "200", description = "Status code") int status,
            @Option(longName = "timeout-config", defaultValue = "0", description = "Limite de timeout em segundos (retorna 408 se exceder)") long timeoutSeconds,
            @Option(longName = "scenario", defaultValue = "", description = "Sequencia stateful de status (ex: 500,500,200)") String scenario,
            @Option(longName = "desc", defaultValue = "", description = "Descricao") String desc) {

        var resolvedBody = resolveBody(body);
        var route = MockRoute.withStatus(HttpMethod.fromString(method), path, status, resolvedBody)
                .withTimeoutSeconds(timeoutSeconds)
                .withScenarioStatusCodes(parseScenarioStatuses(scenario));
        if (desc != null) route = route.withDescription(desc);

        mockService.addRoute(route);

        var sb = new StringBuilder();
        sb.append(styled(GREEN, t("Rota JSON adicionada: ", "JSON route added: ")));
        sb.append(styled(BOLD, route.method() + " " + route.path()));
        sb.append(styled(DIM, " -> " + route.statusCode()));
        if (mockService.isRunning()) {
            sb.append(styled(DIM, "\n  " + t("Teste: ", "Test: ")));
            sb.append(styled(CYAN, mockService.getBaseUrl() + path));
        }
        return sb.toString();
    }

    @Command(name = "mock-add-crud", description = "Cria rotas CRUD completas para um recurso")
    public String mockAddCrud(
            @Option(description = "Path base do recurso (ex: /api/users)") String basePath,
            @Option(longName = "list-body", defaultValue = "[]",
                    description = "Body do GET (lista)") String listBody,
            @Option(longName = "item-body", defaultValue = "{}",
                    description = "Body do GET por ID") String itemBody) {

        var resolvedList = resolveBody(listBody);
        var resolvedItem = resolveBody(itemBody);
        var itemPath = basePath + "/{id}";

        mockService.addRoute(MockRoute.json(HttpMethod.GET, basePath, resolvedList)
                .withDescription(t("Listar ", "List ") + basePath));
        mockService.addRoute(MockRoute.json(HttpMethod.GET, itemPath, resolvedItem)
                .withDescription(t("Buscar " + basePath + " por ID", "Get " + basePath + " by ID")));
        mockService.addRoute(MockRoute.withStatus(HttpMethod.POST, basePath, 201, resolvedItem)
                .withDescription(t("Criar ", "Create ") + basePath));
        mockService.addRoute(MockRoute.json(HttpMethod.PUT, itemPath, resolvedItem)
                .withDescription(t("Atualizar ", "Update ") + basePath));
        mockService.addRoute(MockRoute.withStatus(HttpMethod.DELETE, itemPath, 204, null)
                .withDescription(t("Deletar ", "Delete ") + basePath));

        var sb = new StringBuilder();
        sb.append(styled(BOLD_GREEN, t("CRUD criado para ", "CRUD created for ") + basePath + "\n\n"));
        sb.append(styled(GREEN, "  GET    ")).append(styled(DIM, basePath)).append(styled(DIM, "       -> 200\n"));
        sb.append(styled(GREEN, "  GET    ")).append(styled(DIM, itemPath)).append(styled(DIM, "  -> 200\n"));
        sb.append(styled(YELLOW, "  POST   ")).append(styled(DIM, basePath)).append(styled(DIM, "       -> 201\n"));
        sb.append(styled(CYAN, "  PUT    ")).append(styled(DIM, itemPath)).append(styled(DIM, "  -> 200\n"));
        sb.append(styled(RED, "  DELETE ")).append(styled(DIM, itemPath)).append(styled(DIM, "  -> 204\n"));
        if (mockService.isRunning()) {
            sb.append(styled(DIM, "\n  Base URL: ")).append(styled(CYAN, mockService.getBaseUrl()));
        }
        return sb.toString();
    }

    @Command(name = "mock-from-response", description = "Cria rota mock a partir da ultima resposta recebida")
    public String mockFromResponse(
            @Option(description = "Path da rota no mock (ex: /api/users)") String path,
            @Option(longName = "method", defaultValue = "GET", description = "Metodo HTTP") String method) {

        var lastResponse = apiService.getLastResponse();
        if (lastResponse == null) {
            return styled(RED, t("Nenhuma resposta para usar. Envie um request primeiro.", "No response to use. Send a request first."));
        }

        var headers = new LinkedHashMap<String, String>();
        var ct = lastResponse.contentType();
        if (ct != null && !ct.isBlank()) {
            headers.put("Content-Type", ct);
        }

        var route = new MockRoute(
                HttpMethod.fromString(method), path, lastResponse.statusCode(),
                headers, lastResponse.body(), 0,
                0, java.util.List.of(), t("Criado a partir de resposta real", "Created from real response"));

        mockService.addRoute(route);

        var sb = new StringBuilder();
        sb.append(styled(GREEN, t("Rota criada a partir da ultima resposta:\n", "Route created from last response:\n")));
        sb.append(styled(BOLD, "  " + route.method() + " " + route.path()));
        sb.append(styled(DIM, " -> " + route.statusCode()));
        sb.append(styled(DIM, " (" + lastResponse.sizeText() + ")"));
        if (mockService.isRunning()) {
            sb.append(styled(DIM, "\n  " + t("Teste: ", "Test: "))).append(styled(CYAN, mockService.getBaseUrl() + path));
        }
        return sb.toString();
    }

    @Command(name = "mock-list", description = "Lista todas as rotas do mock server")
    public String mockList() {
        var routes = mockService.listRoutes();
        if (routes.isEmpty()) {
            return styled(DIM, t("Nenhuma rota definida. Use 'mock-add' ou 'mock-add-crud' para criar.", "No routes defined. Use 'mock-add' or 'mock-add-crud' to create."));
        }
        return formatRoutes(routes);
    }

    @Command(name = "mock-show", description = "Mostra detalhes de uma rota")
    public String mockShow(@Option(description = "Indice da rota (1-based)") int index) {
        var route = mockService.getRoute(index);
        if (route.isEmpty()) return styled(RED, t("Indice invalido.", "Invalid index."));
        return formatRouteDetail(index, route.get());
    }

    @Command(name = "mock-edit", description = "Edita uma rota existente (status, body, header, delay)")
    public String mockEdit(
            @Option(description = "Indice da rota (1-based)") int index,
            @Option(longName = "status", defaultValue = "-1", description = "Novo status code") int status,
            @Option(longName = "body", defaultValue = "",
                    description = "Novo body (use @arquivo.json para ler de arquivo)") String body,
            @Option(longName = "header", defaultValue = "",
                    description = "Adicionar headers (formato Key:Value, multiplos separados por ;)") String headers,
            @Option(longName = "delay", defaultValue = "-1", description = "Novo delay em ms") long delay,
            @Option(longName = "timeout-config", defaultValue = "-1", description = "Novo limite de timeout em segundos") long timeoutSeconds,
            @Option(longName = "scenario", defaultValue = "", description = "Nova sequencia stateful de status (ex: 500,500,200)") String scenario,
            @Option(longName = "desc", defaultValue = "", description = "Nova descricao") String desc,
            @Option(longName = "method", defaultValue = "", description = "Novo metodo HTTP") String method,
            @Option(longName = "path", defaultValue = "", description = "Novo path") String path) {

        var existing = mockService.getRoute(index);
        if (existing.isEmpty()) return styled(RED, t("Indice invalido.", "Invalid index."));

        var route = existing.get();
        var changes = new java.util.ArrayList<String>();

        // Apply changes
        var newMethod = route.method();
        if (method != null) {
            newMethod = HttpMethod.fromString(method);
            changes.add(t("metodo: ", "method: ") + newMethod);
        }

        var newPath = route.path();
        if (path != null) {
            newPath = path;
            changes.add("path: " + newPath);
        }

        int newStatus = route.statusCode();
        if (status >= 0) {
            newStatus = status;
            changes.add("status: " + newStatus);
        }

        var newHeaders = new LinkedHashMap<>(route.headers());
        if (headers != null) {
            for (var h : headers.split(";")) {
                var parts = h.split(":", 2);
                if (parts.length == 2) {
                    newHeaders.put(parts[0].trim(), parts[1].trim());
                    changes.add("header: " + parts[0].trim());
                }
            }
        }

        var newBody = route.body();
        if (body != null) {
            newBody = resolveBody(body);
            changes.add(t("body atualizado", "body updated"));
        }

        long newDelay = route.delay();
        if (delay >= 0) {
            newDelay = delay;
            changes.add("delay: " + newDelay + "ms");
        }

        long newTimeout = route.timeoutSeconds();
        if (timeoutSeconds >= 0) {
            newTimeout = timeoutSeconds;
            changes.add(t("timeout-config: ", "timeout-config: ") + newTimeout + "s");
        }

        var newScenario = route.scenarioStatusCodes() != null ? route.scenarioStatusCodes() : java.util.List.<Integer>of();
        if (scenario != null) {
            newScenario = parseScenarioStatuses(scenario);
            changes.add(t("scenario atualizado", "scenario updated"));
        }

        var newDesc = route.description();
        if (desc != null) {
            newDesc = desc;
            changes.add(t("descricao atualizada", "description updated"));
        }

        var updated = new MockRoute(newMethod, newPath, newStatus, newHeaders, newBody, newDelay, newTimeout, newScenario, newDesc);
        mockService.updateRoute(index, updated);

        var sb = new StringBuilder();
        sb.append(styled(GREEN, t("Rota #" + index + " atualizada:\n", "Route #" + index + " updated:\n")));
        sb.append(styled(BOLD, "  " + updated.method() + " " + updated.path()));
        sb.append(styled(DIM, " -> " + updated.statusCode())).append("\n");
        for (var change : changes) {
            sb.append(styled(CYAN, "  + " + change)).append("\n");
        }
        return sb.toString();
    }

    @Command(name = "mock-rm", description = "Remove uma rota do mock server")
    public String mockRm(@Option(description = "Indice da rota (1-based)") int index) {
        if (mockService.removeRoute(index)) {
            return styled(GREEN, t("Rota removida.", "Route removed."));
        }
        return styled(RED, t("Indice invalido.", "Invalid index."));
    }

    @Command(name = "mock-clear", description = "Remove todas as rotas do mock server")
    public String mockClear() {
        mockService.clearRoutes();
        return styled(GREEN, t("Todas as rotas removidas.", "All routes removed."));
    }

    // ========== LOGS =======================================================

    @Command(name = "mock-logs", description = "Mostra requests recebidos pelo mock server")
    public String mockLogs(
            @Option(longName = "limit", defaultValue = "20", description = "Numero de logs") int limit) {
        if (!mockService.isRunning()) {
            return styled(YELLOW, t("Mock server nao esta rodando.", "Mock server is not running."));
        }
        var logs = mockService.getRequestLogs(limit);
        if (logs.isEmpty()) {
            return styled(DIM, t("Nenhum request recebido ainda.", "No requests received yet."));
        }
        return formatLogs(logs);
    }

    @Command(name = "mock-clear-logs", description = "Limpa os logs do mock server")
    public String mockClearLogs() {
        mockService.clearLogs();
        return styled(GREEN, t("Logs limpos.", "Logs cleared."));
    }

    // ========== FORMATTING =================================================

    private String formatRoutes(java.util.List<MockRoute> routes) {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  " + t("Rotas Mock", "Mock Routes")));
        if (mockService.isRunning()) {
            sb.append(styled(DIM, "  (")).append(styled(GREEN, mockService.getBaseUrl())).append(styled(DIM, ")"));
        }
        sb.append("\n\n");

        sb.append(styled(DIM, String.format("  %-4s %-8s %-6s %-26s %-7s %-7s %s%n",
                "#", t("METODO", "METHOD"), "STATUS", "PATH", t("ATRASO", "DELAY"), "TIMEOUT", t("DESCRICAO", "DESCRIPTION"))));
        sb.append(styled(DIM, "  " + "-".repeat(90) + "\n"));

        for (int i = 0; i < routes.size(); i++) {
            var r = routes.get(i);
            var methodStyle = methodColor(r.method());

            sb.append(styled(CYAN, String.format("  %-4d ", i + 1)));
            sb.append(styled(methodStyle, String.format("%-8s ", r.method().name())));
            sb.append(styled(statusColor(r.statusCode()), String.format("%-6d ", r.statusCode())));
            sb.append(styled(BOLD, String.format("%-26s ", truncate(r.path(), 25))));
            sb.append(styled(DIM, String.format("%-7s ", r.delay() > 0 ? r.delay() + "ms" : "-")));
            sb.append(styled(DIM, String.format("%-7s ", r.timeoutSeconds() > 0 ? r.timeoutSeconds() + "s" : "-")));
            sb.append(styled(DIM, r.description() != null ? r.description() : ""));
            sb.append("\n");
        }

        sb.append("\n");
        sb.append(styled(DIM, "  Total: " + routes.size() + " " + t("rota(s)", "route(s)") + "\n"));
        return sb.toString();
    }

    private String formatRouteDetail(int index, MockRoute route) {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  " + t("Rota #", "Route #") + index + "\n\n"));
        sb.append(styled(BOLD, "  " + route.method() + " " + route.path())).append("\n");
        sb.append(styled(DIM, "  " + t("Status", "Status") + ": "))
                .append(styled(statusColor(route.statusCode()), String.valueOf(route.statusCode()))).append("\n");

        if (route.delay() > 0) {
            sb.append(styled(DIM, "  " + t("Atraso", "Delay") + ":  ")).append(styled(YELLOW, route.delay() + "ms")).append("\n");
        }
        if (route.timeoutSeconds() > 0) {
            sb.append(styled(DIM, "  " + t("Timeout-config", "Timeout-config") + ": ")).append(styled(YELLOW, route.timeoutSeconds() + "s")).append("\n");
        }
        if (route.scenarioStatusCodes() != null && !route.scenarioStatusCodes().isEmpty()) {
            sb.append(styled(DIM, "  " + t("Scenario", "Scenario") + ": ")).append(route.scenarioStatusCodes().toString()).append("\n");
        }
        if (route.description() != null) {
            sb.append(styled(DIM, "  " + t("Descricao", "Desc") + ":   ")).append(route.description()).append("\n");
        }

        if (!route.headers().isEmpty()) {
            sb.append(styled(BOLD_CYAN, "\n  " + t("Headers", "Headers") + "\n"));
            for (var h : route.headers().entrySet()) {
                sb.append(styled(GREEN, "  " + h.getKey())).append(styled(DIM, ": ")).append(h.getValue()).append("\n");
            }
        }

        if (route.body() != null && !route.body().isBlank()) {
            sb.append(styled(BOLD_CYAN, "\n  " + t("Body", "Body") + "\n\n"));
            var bodyText = jsonFormatter.isValidJson(route.body())
                    ? jsonFormatter.prettify(route.body()) : route.body();
            for (var line : bodyText.split("\n")) {
                sb.append("  ").append(line).append("\n");
            }
        }

        if (mockService.isRunning()) {
            sb.append(styled(DIM, "\n  " + t("Teste: ", "Test: ")));
            sb.append(styled(CYAN, mockService.getBaseUrl() + route.path())).append("\n");
        }
        return sb.toString();
    }

    private String formatLogs(java.util.List<MockServerService.RequestLog> logs) {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  " + t("Logs do Mock Server", "Mock Server Logs") + "\n\n"));

        sb.append(styled(DIM, String.format("  %-10s %-8s %-6s %-40s %s%n",
                t("HORA", "TIME"), t("METODO", "METHOD"), "STATUS", "PATH", t("MATCH", "MATCH"))));
        sb.append(styled(DIM, "  " + "-".repeat(80) + "\n"));

        for (var log : logs) {
            var methodStyle = switch (log.method().toUpperCase()) {
                case "GET" -> GREEN;
                case "POST" -> YELLOW;
                case "PUT" -> CYAN;
                case "PATCH" -> MAGENTA;
                case "DELETE" -> RED;
                default -> DIM;
            };

            sb.append(styled(DIM, "  " + TIME_FMT.format(log.timestamp()) + " "));
            sb.append(styled(methodStyle, String.format("%-8s ", log.method())));
            sb.append(styled(statusColor(log.statusCode()), String.format("%-6d ", log.statusCode())));
            sb.append(styled(BOLD, String.format("%-40s ", truncate(log.path(), 39))));
            sb.append(styled(log.matched() ? GREEN : RED, log.matched() ? "OK" : t("FALHOU", "MISS")));
            sb.append("\n");
        }
        return sb.toString();
    }

    // ========== HELPERS ====================================================

    private LinkedHashMap<String, String> parseHeaders(String headers) {
        var map = new LinkedHashMap<String, String>();
        map.put("Content-Type", "application/json");
        if (headers != null) {
            for (var h : headers.split(";")) {
                var parts = h.split(":", 2);
                if (parts.length == 2) {
                    map.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return map;
    }

    private String resolveBody(String body) {
        if (body == null) return null;
        if (body.startsWith("@")) {
            try {
                var path = Path.of(body.substring(1));
                return Files.readString(path);
            } catch (IOException e) {
                return body;
            }
        }
        return body;
    }

    private java.util.List<Integer> parseScenarioStatuses(String scenario) {
        if (scenario == null || scenario.isBlank()) return java.util.List.of();
        var statuses = new java.util.ArrayList<Integer>();
        for (var part : scenario.split(",")) {
            var trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            try {
                statuses.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException ignored) {
                // Ignore invalid status chunks to keep command resilient
            }
        }
        return statuses;
    }

    private org.jline.utils.AttributedStyle methodColor(HttpMethod method) {
        return switch (method) {
            case GET -> GREEN;
            case POST -> YELLOW;
            case PUT -> CYAN;
            case PATCH -> MAGENTA;
            case DELETE -> RED;
            default -> DIM;
        };
    }

    private org.jline.utils.AttributedStyle statusColor(int status) {
        if (status < 300) return GREEN;
        if (status < 400) return YELLOW;
        return RED;
    }
}
