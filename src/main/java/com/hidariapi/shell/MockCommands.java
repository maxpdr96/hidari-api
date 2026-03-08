package com.hidariapi.shell;

import com.hidariapi.model.HttpMethod;
import com.hidariapi.model.MockRoute;
import com.hidariapi.service.ApiService;
import com.hidariapi.service.MockServerService;
import com.hidariapi.util.JsonFormatter;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

/**
 * Comandos do Mock Server — cria APIs fake direto no terminal.
 */
@ShellComponent
public class MockCommands {

    private static final AttributedStyle BOLD = AttributedStyle.DEFAULT.bold();
    private static final AttributedStyle DIM = AttributedStyle.DEFAULT.faint();
    private static final AttributedStyle CYAN = AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
    private static final AttributedStyle GREEN = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
    private static final AttributedStyle YELLOW = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
    private static final AttributedStyle RED = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
    private static final AttributedStyle MAGENTA = AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA);
    private static final AttributedStyle BOLD_CYAN = BOLD.foreground(AttributedStyle.CYAN);
    private static final AttributedStyle BOLD_GREEN = BOLD.foreground(AttributedStyle.GREEN);
    private static final AttributedStyle BOLD_RED = BOLD.foreground(AttributedStyle.RED);
    private static final AttributedStyle BOLD_YELLOW = BOLD.foreground(AttributedStyle.YELLOW);

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final MockServerService mockService;
    private final ApiService apiService;
    private final JsonFormatter jsonFormatter;

    public MockCommands(MockServerService mockService, ApiService apiService, JsonFormatter jsonFormatter) {
        this.mockService = mockService;
        this.apiService = apiService;
        this.jsonFormatter = jsonFormatter;
    }

    // ========== SERVER CONTROL =============================================

    @ShellMethod(key = "mock-start", value = "Inicia o mock server")
    public String mockStart(
            @ShellOption(value = "--port", defaultValue = "8089", help = "Porta do servidor") int port) {
        try {
            mockService.start(port);
            var sb = new StringBuilder();
            sb.append(styled(BOLD_GREEN, "Mock server iniciado!\n\n"));
            sb.append(styled(DIM, "  Base URL: "));
            sb.append(styled(CYAN, mockService.getBaseUrl())).append("\n");
            sb.append(styled(DIM, "  Rotas:    "));
            sb.append(styled(CYAN, String.valueOf(mockService.routeCount()))).append("\n\n");
            sb.append(styled(DIM, "  Use 'mock-add' para criar rotas e 'mock-logs' para ver requests recebidos.\n"));
            return sb.toString();
        } catch (IOException e) {
            return styled(RED, "Erro ao iniciar: " + e.getMessage());
        }
    }

    @ShellMethod(key = "mock-stop", value = "Para o mock server")
    public String mockStop() {
        if (!mockService.isRunning()) {
            return styled(YELLOW, "Mock server nao esta rodando.");
        }
        mockService.stop();
        return styled(GREEN, "Mock server parado.");
    }

    @ShellMethod(key = "mock-status", value = "Mostra status do mock server")
    public String mockStatus() {
        if (!mockService.isRunning()) {
            return styled(DIM, "Mock server nao esta rodando. Use 'mock-start' para iniciar.");
        }
        var sb = new StringBuilder();
        sb.append(styled(BOLD_GREEN, "\n  Mock Server - Ativo\n\n"));
        sb.append(styled(DIM, "  Base URL: ")).append(styled(CYAN, mockService.getBaseUrl())).append("\n");
        sb.append(styled(DIM, "  Porta:    ")).append(styled(CYAN, String.valueOf(mockService.getPort()))).append("\n");
        sb.append(styled(DIM, "  Rotas:    ")).append(styled(CYAN, String.valueOf(mockService.routeCount()))).append("\n");
        return sb.toString();
    }

    // ========== ROUTE MANAGEMENT ===========================================

    @ShellMethod(key = "mock-add", value = "Adiciona uma rota ao mock server")
    public String mockAdd(
            @ShellOption(help = "Metodo HTTP (GET, POST, PUT, DELETE, etc.)") String method,
            @ShellOption(help = "Path da rota (ex: /api/users, /api/users/{id})") String path,
            @ShellOption(value = "--status", defaultValue = "200", help = "Status code da resposta") int status,
            @ShellOption(value = "--body", defaultValue = ShellOption.NULL,
                    help = "Body da resposta (use @arquivo.json para ler de arquivo)") String body,
            @ShellOption(value = "--header", defaultValue = ShellOption.NULL,
                    help = "Headers (formato Key:Value, multiplos separados por ;)") String headers,
            @ShellOption(value = "--delay", defaultValue = "0", help = "Delay em ms antes de responder") long delay,
            @ShellOption(value = "--desc", defaultValue = ShellOption.NULL, help = "Descricao da rota") String desc) {

        var resolvedBody = resolveBody(body);
        var route = new MockRoute(
                HttpMethod.fromString(method), path, status,
                parseHeaders(headers), resolvedBody, delay, desc);

        mockService.addRoute(route);

        var sb = new StringBuilder();
        sb.append(styled(GREEN, "Rota adicionada: "));
        sb.append(styled(BOLD, route.method() + " " + route.path()));
        sb.append(styled(DIM, " -> " + route.statusCode()));
        if (mockService.isRunning()) {
            sb.append(styled(DIM, "\n  Teste: "));
            sb.append(styled(CYAN, mockService.getBaseUrl() + path));
        }
        return sb.toString();
    }

    @ShellMethod(key = "mock-add-json", value = "Adiciona rota que retorna JSON (atalho)")
    public String mockAddJson(
            @ShellOption(help = "Metodo HTTP") String method,
            @ShellOption(help = "Path da rota") String path,
            @ShellOption(value = "--body", help = "Body JSON (use @arquivo.json para ler de arquivo)") String body,
            @ShellOption(value = "--status", defaultValue = "200", help = "Status code") int status,
            @ShellOption(value = "--desc", defaultValue = ShellOption.NULL, help = "Descricao") String desc) {

        var resolvedBody = resolveBody(body);
        var route = MockRoute.withStatus(HttpMethod.fromString(method), path, status, resolvedBody);
        if (desc != null) route = route.withDescription(desc);

        mockService.addRoute(route);

        var sb = new StringBuilder();
        sb.append(styled(GREEN, "Rota JSON adicionada: "));
        sb.append(styled(BOLD, route.method() + " " + route.path()));
        sb.append(styled(DIM, " -> " + route.statusCode()));
        if (mockService.isRunning()) {
            sb.append(styled(DIM, "\n  Teste: "));
            sb.append(styled(CYAN, mockService.getBaseUrl() + path));
        }
        return sb.toString();
    }

    @ShellMethod(key = "mock-add-crud", value = "Cria rotas CRUD completas para um recurso")
    public String mockAddCrud(
            @ShellOption(help = "Path base do recurso (ex: /api/users)") String basePath,
            @ShellOption(value = "--list-body", defaultValue = "[]",
                    help = "Body do GET (lista)") String listBody,
            @ShellOption(value = "--item-body", defaultValue = "{}",
                    help = "Body do GET por ID") String itemBody) {

        var resolvedList = resolveBody(listBody);
        var resolvedItem = resolveBody(itemBody);
        var itemPath = basePath + "/{id}";

        mockService.addRoute(MockRoute.json(HttpMethod.GET, basePath, resolvedList)
                .withDescription("Listar " + basePath));
        mockService.addRoute(MockRoute.json(HttpMethod.GET, itemPath, resolvedItem)
                .withDescription("Buscar " + basePath + " por ID"));
        mockService.addRoute(MockRoute.withStatus(HttpMethod.POST, basePath, 201, resolvedItem)
                .withDescription("Criar " + basePath));
        mockService.addRoute(MockRoute.json(HttpMethod.PUT, itemPath, resolvedItem)
                .withDescription("Atualizar " + basePath));
        mockService.addRoute(MockRoute.withStatus(HttpMethod.DELETE, itemPath, 204, null)
                .withDescription("Deletar " + basePath));

        var sb = new StringBuilder();
        sb.append(styled(BOLD_GREEN, "CRUD criado para " + basePath + "\n\n"));
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

    @ShellMethod(key = "mock-from-response", value = "Cria rota mock a partir da ultima resposta recebida")
    public String mockFromResponse(
            @ShellOption(help = "Path da rota no mock (ex: /api/users)") String path,
            @ShellOption(value = "--method", defaultValue = "GET", help = "Metodo HTTP") String method) {

        var lastResponse = apiService.getLastResponse();
        if (lastResponse == null) {
            return styled(RED, "Nenhuma resposta para usar. Envie um request primeiro.");
        }

        var headers = new LinkedHashMap<String, String>();
        var ct = lastResponse.contentType();
        if (ct != null && !ct.isBlank()) {
            headers.put("Content-Type", ct);
        }

        var route = new MockRoute(
                HttpMethod.fromString(method), path, lastResponse.statusCode(),
                headers, lastResponse.body(), 0,
                "Criado a partir de resposta real");

        mockService.addRoute(route);

        var sb = new StringBuilder();
        sb.append(styled(GREEN, "Rota criada a partir da ultima resposta:\n"));
        sb.append(styled(BOLD, "  " + route.method() + " " + route.path()));
        sb.append(styled(DIM, " -> " + route.statusCode()));
        sb.append(styled(DIM, " (" + lastResponse.sizeText() + ")"));
        if (mockService.isRunning()) {
            sb.append(styled(DIM, "\n  Teste: ")).append(styled(CYAN, mockService.getBaseUrl() + path));
        }
        return sb.toString();
    }

    @ShellMethod(key = "mock-list", value = "Lista todas as rotas do mock server")
    public String mockList() {
        var routes = mockService.listRoutes();
        if (routes.isEmpty()) {
            return styled(DIM, "Nenhuma rota definida. Use 'mock-add' ou 'mock-add-crud' para criar.");
        }
        return formatRoutes(routes);
    }

    @ShellMethod(key = "mock-show", value = "Mostra detalhes de uma rota")
    public String mockShow(@ShellOption(help = "Indice da rota (1-based)") int index) {
        var route = mockService.getRoute(index);
        if (route.isEmpty()) return styled(RED, "Indice invalido.");
        return formatRouteDetail(index, route.get());
    }

    @ShellMethod(key = "mock-edit", value = "Edita uma rota existente (status, body, header, delay)")
    public String mockEdit(
            @ShellOption(help = "Indice da rota (1-based)") int index,
            @ShellOption(value = "--status", defaultValue = "-1", help = "Novo status code") int status,
            @ShellOption(value = "--body", defaultValue = ShellOption.NULL,
                    help = "Novo body (use @arquivo.json para ler de arquivo)") String body,
            @ShellOption(value = "--header", defaultValue = ShellOption.NULL,
                    help = "Adicionar headers (formato Key:Value, multiplos separados por ;)") String headers,
            @ShellOption(value = "--delay", defaultValue = "-1", help = "Novo delay em ms") long delay,
            @ShellOption(value = "--desc", defaultValue = ShellOption.NULL, help = "Nova descricao") String desc,
            @ShellOption(value = "--method", defaultValue = ShellOption.NULL, help = "Novo metodo HTTP") String method,
            @ShellOption(value = "--path", defaultValue = ShellOption.NULL, help = "Novo path") String path) {

        var existing = mockService.getRoute(index);
        if (existing.isEmpty()) return styled(RED, "Indice invalido.");

        var route = existing.get();
        var changes = new java.util.ArrayList<String>();

        // Aplicar alteracoes
        var newMethod = route.method();
        if (method != null) {
            newMethod = HttpMethod.fromString(method);
            changes.add("metodo: " + newMethod);
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
            changes.add("body atualizado");
        }

        long newDelay = route.delay();
        if (delay >= 0) {
            newDelay = delay;
            changes.add("delay: " + newDelay + "ms");
        }

        var newDesc = route.description();
        if (desc != null) {
            newDesc = desc;
            changes.add("descricao atualizada");
        }

        var updated = new MockRoute(newMethod, newPath, newStatus, newHeaders, newBody, newDelay, newDesc);
        mockService.updateRoute(index, updated);

        var sb = new StringBuilder();
        sb.append(styled(GREEN, "Rota #" + index + " atualizada:\n"));
        sb.append(styled(BOLD, "  " + updated.method() + " " + updated.path()));
        sb.append(styled(DIM, " -> " + updated.statusCode())).append("\n");
        for (var change : changes) {
            sb.append(styled(CYAN, "  + " + change)).append("\n");
        }
        return sb.toString();
    }

    @ShellMethod(key = "mock-rm", value = "Remove uma rota do mock server")
    public String mockRm(@ShellOption(help = "Indice da rota (1-based)") int index) {
        if (mockService.removeRoute(index)) {
            return styled(GREEN, "Rota removida.");
        }
        return styled(RED, "Indice invalido.");
    }

    @ShellMethod(key = "mock-clear", value = "Remove todas as rotas do mock server")
    public String mockClear() {
        mockService.clearRoutes();
        return styled(GREEN, "Todas as rotas removidas.");
    }

    // ========== LOGS =======================================================

    @ShellMethod(key = "mock-logs", value = "Mostra requests recebidos pelo mock server")
    public String mockLogs(
            @ShellOption(value = "--limit", defaultValue = "20", help = "Numero de logs") int limit) {
        if (!mockService.isRunning()) {
            return styled(YELLOW, "Mock server nao esta rodando.");
        }
        var logs = mockService.getRequestLogs(limit);
        if (logs.isEmpty()) {
            return styled(DIM, "Nenhum request recebido ainda.");
        }
        return formatLogs(logs);
    }

    @ShellMethod(key = "mock-clear-logs", value = "Limpa os logs do mock server")
    public String mockClearLogs() {
        mockService.clearLogs();
        return styled(GREEN, "Logs limpos.");
    }

    // ========== FORMATTING =================================================

    private String formatRoutes(java.util.List<MockRoute> routes) {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  Mock Routes"));
        if (mockService.isRunning()) {
            sb.append(styled(DIM, "  (")).append(styled(GREEN, mockService.getBaseUrl())).append(styled(DIM, ")"));
        }
        sb.append("\n\n");

        sb.append(styled(DIM, String.format("  %-4s %-8s %-6s %-30s %-5s %s%n",
                "#", "METODO", "STATUS", "PATH", "DELAY", "DESCRICAO")));
        sb.append(styled(DIM, "  " + "-".repeat(90) + "\n"));

        for (int i = 0; i < routes.size(); i++) {
            var r = routes.get(i);
            var methodStyle = methodColor(r.method());

            sb.append(styled(CYAN, String.format("  %-4d ", i + 1)));
            sb.append(styled(methodStyle, String.format("%-8s ", r.method().name())));
            sb.append(styled(statusColor(r.statusCode()), String.format("%-6d ", r.statusCode())));
            sb.append(styled(BOLD, String.format("%-30s ", truncate(r.path(), 29))));
            sb.append(styled(DIM, String.format("%-5s ", r.delay() > 0 ? r.delay() + "ms" : "-")));
            sb.append(styled(DIM, r.description() != null ? r.description() : ""));
            sb.append("\n");
        }

        sb.append("\n");
        sb.append(styled(DIM, "  Total: " + routes.size() + " rota(s)\n"));
        return sb.toString();
    }

    private String formatRouteDetail(int index, MockRoute route) {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  Rota #" + index + "\n\n"));
        sb.append(styled(BOLD, "  " + route.method() + " " + route.path())).append("\n");
        sb.append(styled(DIM, "  Status: ")).append(styled(statusColor(route.statusCode()), String.valueOf(route.statusCode()))).append("\n");

        if (route.delay() > 0) {
            sb.append(styled(DIM, "  Delay:  ")).append(styled(YELLOW, route.delay() + "ms")).append("\n");
        }
        if (route.description() != null) {
            sb.append(styled(DIM, "  Desc:   ")).append(route.description()).append("\n");
        }

        if (!route.headers().isEmpty()) {
            sb.append(styled(BOLD_CYAN, "\n  Headers\n"));
            for (var h : route.headers().entrySet()) {
                sb.append(styled(GREEN, "  " + h.getKey())).append(styled(DIM, ": ")).append(h.getValue()).append("\n");
            }
        }

        if (route.body() != null && !route.body().isBlank()) {
            sb.append(styled(BOLD_CYAN, "\n  Body\n\n"));
            var bodyText = jsonFormatter.isValidJson(route.body())
                    ? jsonFormatter.prettify(route.body()) : route.body();
            for (var line : bodyText.split("\n")) {
                sb.append("  ").append(line).append("\n");
            }
        }

        if (mockService.isRunning()) {
            sb.append(styled(DIM, "\n  Teste: "));
            sb.append(styled(CYAN, mockService.getBaseUrl() + route.path())).append("\n");
        }
        return sb.toString();
    }

    private String formatLogs(java.util.List<MockServerService.RequestLog> logs) {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  Mock Server Logs\n\n"));

        sb.append(styled(DIM, String.format("  %-10s %-8s %-6s %-40s %s%n",
                "HORA", "METODO", "STATUS", "PATH", "MATCH")));
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
            sb.append(styled(log.matched() ? GREEN : RED, log.matched() ? "OK" : "MISS"));
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

    private AttributedStyle methodColor(HttpMethod method) {
        return switch (method) {
            case GET -> GREEN;
            case POST -> YELLOW;
            case PUT -> CYAN;
            case PATCH -> MAGENTA;
            case DELETE -> RED;
            default -> DIM;
        };
    }

    private AttributedStyle statusColor(int status) {
        if (status < 300) return GREEN;
        if (status < 400) return YELLOW;
        return RED;
    }

    private String styled(AttributedStyle style, String text) {
        return new AttributedStringBuilder()
                .style(style)
                .append(text)
                .toAttributedString()
                .toAnsi();
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max - 1) + "~" : text;
    }
}
