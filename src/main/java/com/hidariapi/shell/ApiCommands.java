package com.hidariapi.shell;

import com.hidariapi.model.*;
import com.hidariapi.model.Collection;
import com.hidariapi.service.ApiService;
import com.hidariapi.util.CurlParser;
import com.hidariapi.util.JsonFormatter;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.util.*;

/**
 * Comandos interativos do HidariApi — testador de APIs no terminal.
 */
@ShellComponent
public class ApiCommands {

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
    private static final AttributedStyle WHITE = AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE);

    private final ApiService service;
    private final JsonFormatter jsonFormatter;
    private final CurlParser curlParser;

    public ApiCommands(ApiService service, JsonFormatter jsonFormatter, CurlParser curlParser) {
        this.service = service;
        this.jsonFormatter = jsonFormatter;
        this.curlParser = curlParser;
    }

    // ========== HTTP REQUESTS ==============================================

    @ShellMethod(key = "get", value = "Envia requisicao GET")
    public String get(
            @ShellOption(help = "URL") String url,
            @ShellOption(value = "--param", defaultValue = ShellOption.NULL,
                    help = "Query params (formato key=value, multiplos separados por &)") String params) {
        return executeRequest(ApiRequest.of(null, HttpMethod.GET, appendParams(url, params)));
    }

    @ShellMethod(key = "post", value = "Envia requisicao POST com body JSON")
    public String post(
            @ShellOption(help = "URL") String url,
            @ShellOption(value = "--body", defaultValue = ShellOption.NULL,
                    help = "Body JSON (use @arquivo.json para ler de arquivo)") String body,
            @ShellOption(value = "--param", defaultValue = ShellOption.NULL,
                    help = "Query params (formato key=value, multiplos separados por &)") String params) {
        var req = ApiRequest.of(null, HttpMethod.POST, appendParams(url, params))
                .withHeader("Content-Type", "application/json");
        req = resolveAndSetBody(req, body);
        if (req == null) return styled(RED, "Erro ao ler body do arquivo.");
        return executeRequest(req);
    }

    @ShellMethod(key = "put", value = "Envia requisicao PUT com body JSON")
    public String put(
            @ShellOption(help = "URL") String url,
            @ShellOption(value = "--body", defaultValue = ShellOption.NULL,
                    help = "Body JSON (use @arquivo.json para ler de arquivo)") String body,
            @ShellOption(value = "--param", defaultValue = ShellOption.NULL,
                    help = "Query params (formato key=value, multiplos separados por &)") String params) {
        var req = ApiRequest.of(null, HttpMethod.PUT, appendParams(url, params))
                .withHeader("Content-Type", "application/json");
        req = resolveAndSetBody(req, body);
        if (req == null) return styled(RED, "Erro ao ler body do arquivo.");
        return executeRequest(req);
    }

    @ShellMethod(key = "patch", value = "Envia requisicao PATCH com body JSON")
    public String patch(
            @ShellOption(help = "URL") String url,
            @ShellOption(value = "--body", defaultValue = ShellOption.NULL,
                    help = "Body JSON (use @arquivo.json para ler de arquivo)") String body,
            @ShellOption(value = "--param", defaultValue = ShellOption.NULL,
                    help = "Query params (formato key=value, multiplos separados por &)") String params) {
        var req = ApiRequest.of(null, HttpMethod.PATCH, appendParams(url, params))
                .withHeader("Content-Type", "application/json");
        req = resolveAndSetBody(req, body);
        if (req == null) return styled(RED, "Erro ao ler body do arquivo.");
        return executeRequest(req);
    }

    @ShellMethod(key = "delete", value = "Envia requisicao DELETE")
    public String delete(
            @ShellOption(help = "URL") String url,
            @ShellOption(value = "--param", defaultValue = ShellOption.NULL,
                    help = "Query params (formato key=value, multiplos separados por &)") String params) {
        return executeRequest(ApiRequest.of(null, HttpMethod.DELETE, appendParams(url, params)));
    }

    @ShellMethod(key = "head", value = "Envia requisicao HEAD (retorna apenas headers)")
    public String head(@ShellOption(help = "URL") String url) {
        return executeRequest(ApiRequest.of(null, HttpMethod.HEAD, url));
    }

    @ShellMethod(key = "options", value = "Envia requisicao OPTIONS")
    public String options(@ShellOption(help = "URL") String url) {
        return executeRequest(ApiRequest.of(null, HttpMethod.OPTIONS, url));
    }

    @ShellMethod(key = "send", value = "Envia request customizado (metodo, url, headers, body)")
    public String send(
            @ShellOption(help = "Metodo HTTP (GET, POST, PUT, etc.)") String method,
            @ShellOption(help = "URL") String url,
            @ShellOption(value = "--header", defaultValue = ShellOption.NULL,
                    help = "Header (formato Key:Value, multiplos separados por ;)") String headers,
            @ShellOption(value = "--body", defaultValue = ShellOption.NULL,
                    help = "Body (use @arquivo.json para ler de arquivo)") String body,
            @ShellOption(value = "--param", defaultValue = ShellOption.NULL,
                    help = "Query params (formato key=value, multiplos separados por &)") String params) {
        var req = ApiRequest.of(null, HttpMethod.fromString(method), appendParams(url, params));
        if (headers != null) {
            for (var h : headers.split(";")) {
                var parts = h.split(":", 2);
                if (parts.length == 2) {
                    req = req.withHeader(parts[0].trim(), parts[1].trim());
                }
            }
        }
        req = resolveAndSetBody(req, body);
        if (req == null) return styled(RED, "Erro ao ler body do arquivo.");
        return executeRequest(req);
    }

    // ========== RESPONSE ===================================================

    @ShellMethod(key = "response", value = "Mostra a ultima resposta completa")
    public String response() {
        var res = service.getLastResponse();
        if (res == null) return styled(YELLOW, "Nenhuma resposta ainda. Envie um request primeiro.");
        return formatFullResponse(service.getLastRequest(), res);
    }

    @ShellMethod(key = "body", value = "Mostra apenas o body da ultima resposta")
    public String body() {
        var res = service.getLastResponse();
        if (res == null) return styled(YELLOW, "Nenhuma resposta ainda.");
        if (res.body().isBlank()) return styled(DIM, "(body vazio)");
        if (res.isJson()) return jsonFormatter.prettify(res.body());
        return res.body();
    }

    @ShellMethod(key = "response-headers", value = "Mostra headers da ultima resposta")
    public String responseHeaders() {
        var res = service.getLastResponse();
        if (res == null) return styled(YELLOW, "Nenhuma resposta ainda.");
        return formatHeaders("Response Headers", res.headers());
    }

    @ShellMethod(key = "curl", value = "Gera comando cURL do ultimo request")
    public String curl() {
        var req = service.getLastRequest();
        if (req == null) return styled(YELLOW, "Nenhum request ainda.");
        return styled(CYAN, req.toCurl());
    }

    @ShellMethod(key = "import-curl", value = "Importa um comando cURL e executa como request")
    public String importCurl(
            @ShellOption(help = "Comando cURL completo (entre aspas)") String curlCommand,
            @ShellOption(value = "--save", defaultValue = ShellOption.NULL,
                    help = "Salvar na colecao (formato colecao:nome)") String save,
            @ShellOption(value = "--dry-run", defaultValue = "false",
                    help = "Apenas parseia sem executar") boolean dryRun) {
        try {
            var req = curlParser.parse(curlCommand);

            if (dryRun) {
                var sb = new StringBuilder();
                sb.append(styled(BOLD_CYAN, "\n  Request parseado do cURL\n\n"));
                sb.append(styled(BOLD, "  " + req.method() + " ")).append(req.url()).append("\n");
                if (!req.headers().isEmpty()) {
                    for (var h : req.headers().entrySet()) {
                        sb.append(styled(DIM, "  " + h.getKey() + ": ")).append(h.getValue()).append("\n");
                    }
                }
                if (req.body() != null && !req.body().isBlank()) {
                    sb.append(styled(DIM, "\n  Body:\n"));
                    var bodyText = jsonFormatter.isValidJson(req.body())
                            ? jsonFormatter.prettify(req.body()) : req.body();
                    for (var line : bodyText.split("\n")) {
                        sb.append("  ").append(line).append("\n");
                    }
                }
                return sb.toString();
            }

            // Salvar em colecao se pedido
            if (save != null) {
                var parts = save.split(":", 2);
                if (parts.length == 2) {
                    var col = service.getCollection(parts[0]);
                    if (col.isPresent()) {
                        service.saveCollection(col.get().withRequest(req.withName(parts[1])));
                        System.out.println(styled(GREEN,
                                "Salvo como '" + parts[1] + "' na colecao '" + parts[0] + "'"));
                    }
                }
            }

            return executeRequest(req);
        } catch (Exception e) {
            return styled(RED, "Erro ao parsear cURL: " + e.getMessage());
        }
    }

    @ShellMethod(key = "save-response", value = "Salva o body da ultima resposta em um arquivo")
    public String saveResponse(@ShellOption(help = "Caminho do arquivo") String filePath) {
        try {
            service.saveResponseToFile(filePath);
            var res = service.getLastResponse();
            return styled(GREEN, "Resposta salva em: " + filePath + " (" + res.sizeText() + ")");
        } catch (IOException e) {
            return styled(RED, "Erro ao salvar: " + e.getMessage());
        }
    }

    // ========== DEFAULT HEADERS ============================================

    @ShellMethod(key = "set-header", value = "Define header padrao para todos os requests")
    public String setHeader(
            @ShellOption(help = "Nome do header") String key,
            @ShellOption(help = "Valor do header") String value) {
        service.setDefaultHeader(key, value);
        return styled(GREEN, "Header definido: " + key + ": " + value);
    }

    @ShellMethod(key = "unset-header", value = "Remove header padrao")
    public String unsetHeader(@ShellOption(help = "Nome do header") String key) {
        service.removeDefaultHeader(key);
        return styled(GREEN, "Header removido: " + key);
    }

    @ShellMethod(key = "headers", value = "Lista headers padrao configurados")
    public String headers() {
        var hdrs = service.getDefaultHeaders();
        if (hdrs.isEmpty()) return styled(DIM, "Nenhum header padrao configurado.");
        return formatHeaders("Headers Padrao", hdrs);
    }

    @ShellMethod(key = "clear-headers", value = "Remove todos os headers padrao")
    public String clearHeaders() {
        service.clearDefaultHeaders();
        return styled(GREEN, "Todos os headers padrao removidos.");
    }

    @ShellMethod(key = "bearer", value = "Define Authorization: Bearer <token>")
    public String bearer(@ShellOption(help = "Token") String token) {
        service.setDefaultHeader("Authorization", "Bearer " + token);
        return styled(GREEN, "Bearer token configurado.");
    }

    // ========== ENVIRONMENTS ===============================================

    @ShellMethod(key = "env-create", value = "Cria um novo ambiente")
    public String envCreate(@ShellOption(help = "Nome do ambiente (ex: dev, prod)") String name) {
        service.saveEnvironment(Environment.empty(name));
        return styled(GREEN, "Ambiente criado: " + name);
    }

    @ShellMethod(key = "env-set", value = "Define variavel no ambiente")
    public String envSet(
            @ShellOption(help = "Nome do ambiente") String envName,
            @ShellOption(help = "Nome da variavel") String key,
            @ShellOption(help = "Valor") String value) {
        var env = service.getEnvironment(envName);
        if (env.isEmpty()) return styled(RED, "Ambiente nao encontrado: " + envName);
        service.saveEnvironment(env.get().withVariable(key, value));
        return styled(GREEN, "Variavel definida: " + key + " = " + value + " [" + envName + "]");
    }

    @ShellMethod(key = "env-unset", value = "Remove variavel de um ambiente")
    public String envUnset(
            @ShellOption(help = "Nome do ambiente") String envName,
            @ShellOption(help = "Nome da variavel") String key) {
        var env = service.getEnvironment(envName);
        if (env.isEmpty()) return styled(RED, "Ambiente nao encontrado: " + envName);
        service.saveEnvironment(env.get().withoutVariable(key));
        return styled(GREEN, "Variavel removida: " + key + " [" + envName + "]");
    }

    @ShellMethod(key = "env-use", value = "Ativa um ambiente (variaveis {{key}} serao substituidas)")
    public String envUse(@ShellOption(help = "Nome do ambiente") String name) {
        var env = service.getEnvironment(name);
        if (env.isEmpty()) return styled(RED, "Ambiente nao encontrado: " + name);
        service.setActiveEnvironment(name);
        return styled(GREEN, "Ambiente ativo: " + name);
    }

    @ShellMethod(key = "env-clear", value = "Desativa o ambiente atual")
    public String envClear() {
        service.clearActiveEnvironment();
        return styled(GREEN, "Ambiente desativado.");
    }

    @ShellMethod(key = "envs", value = "Lista ambientes disponiveis")
    public String envs() {
        var envs = service.listEnvironments();
        if (envs.isEmpty()) return styled(DIM, "Nenhum ambiente criado. Use 'env-create <nome>' para criar.");
        return formatEnvironments(envs);
    }

    @ShellMethod(key = "env-show", value = "Mostra variaveis de um ambiente")
    public String envShow(@ShellOption(help = "Nome do ambiente") String name) {
        var env = service.getEnvironment(name);
        if (env.isEmpty()) return styled(RED, "Ambiente nao encontrado: " + name);
        return formatEnvironmentDetail(env.get());
    }

    @ShellMethod(key = "env-rm", value = "Remove um ambiente")
    public String envRm(@ShellOption(help = "Nome do ambiente") String name) {
        if (service.removeEnvironment(name)) {
            return styled(GREEN, "Ambiente removido: " + name);
        }
        return styled(RED, "Ambiente nao encontrado: " + name);
    }

    // ========== COLLECTIONS ================================================

    @ShellMethod(key = "col-create", value = "Cria uma nova colecao")
    public String colCreate(@ShellOption(help = "Nome da colecao") String name) {
        service.saveCollection(Collection.empty(name));
        return styled(GREEN, "Colecao criada: " + name);
    }

    @ShellMethod(key = "col-add", value = "Salva o ultimo request em uma colecao")
    public String colAdd(
            @ShellOption(help = "Nome da colecao") String colName,
            @ShellOption(help = "Nome para o request") String reqName) {
        var req = service.getLastRequest();
        if (req == null) return styled(RED, "Nenhum request para salvar. Envie um request primeiro.");
        var col = service.getCollection(colName);
        if (col.isEmpty()) return styled(RED, "Colecao nao encontrada: " + colName);
        service.saveCollection(col.get().withRequest(req.withName(reqName)));
        return styled(GREEN, "Request '" + reqName + "' salvo na colecao '" + colName + "'");
    }

    @ShellMethod(key = "col-run", value = "Executa um request de uma colecao")
    public String colRun(
            @ShellOption(help = "Nome da colecao") String colName,
            @ShellOption(help = "Indice do request (1-based)") int index) {
        var col = service.getCollection(colName);
        if (col.isEmpty()) return styled(RED, "Colecao nao encontrada: " + colName);
        var requests = col.get().requests();
        if (index < 1 || index > requests.size()) {
            return styled(RED, "Indice invalido. Colecao tem " + requests.size() + " request(s).");
        }
        return executeRequest(requests.get(index - 1));
    }

    @ShellMethod(key = "col-show", value = "Mostra requests de uma colecao")
    public String colShow(@ShellOption(help = "Nome da colecao") String name) {
        var col = service.getCollection(name);
        if (col.isEmpty()) return styled(RED, "Colecao nao encontrada: " + name);
        return formatCollectionDetail(col.get());
    }

    @ShellMethod(key = "col-rm-req", value = "Remove um request de uma colecao")
    public String colRmReq(
            @ShellOption(help = "Nome da colecao") String colName,
            @ShellOption(help = "Indice do request (1-based)") int index) {
        var col = service.getCollection(colName);
        if (col.isEmpty()) return styled(RED, "Colecao nao encontrada: " + colName);
        if (index < 1 || index > col.get().requests().size()) {
            return styled(RED, "Indice invalido.");
        }
        service.saveCollection(col.get().withoutRequest(index - 1));
        return styled(GREEN, "Request removido da colecao '" + colName + "'");
    }

    @ShellMethod(key = "cols", value = "Lista colecoes")
    public String cols() {
        var cols = service.listCollections();
        if (cols.isEmpty()) return styled(DIM, "Nenhuma colecao. Use 'col-create <nome>' para criar.");
        return formatCollections(cols);
    }

    @ShellMethod(key = "col-run-all", value = "Executa todos os requests de uma colecao (smoke test)")
    public String colRunAll(@ShellOption(help = "Nome da colecao") String colName) {
        var col = service.getCollection(colName);
        if (col.isEmpty()) return styled(RED, "Colecao nao encontrada: " + colName);
        if (col.get().requests().isEmpty()) return styled(YELLOW, "Colecao vazia.");

        System.out.println(styled(CYAN, "Executando " + col.get().requests().size()
                + " request(s) da colecao '" + colName + "'...\n"));

        var results = service.runCollection(colName);
        return formatRunAllResults(colName, results);
    }

    @ShellMethod(key = "col-rm", value = "Remove uma colecao")
    public String colRm(@ShellOption(help = "Nome da colecao") String name) {
        if (service.removeCollection(name)) {
            return styled(GREEN, "Colecao removida: " + name);
        }
        return styled(RED, "Colecao nao encontrada: " + name);
    }

    // ========== HISTORY ====================================================

    @ShellMethod(key = "history", value = "Mostra historico de requests")
    public String history(
            @ShellOption(value = "--limit", defaultValue = "20", help = "Limite de entradas") int limit) {
        var entries = service.getHistory(limit);
        if (entries.isEmpty()) return styled(DIM, "Historico vazio.");
        return formatHistory(entries);
    }

    @ShellMethod(key = "replay", value = "Re-executa um request do historico")
    public String replay(@ShellOption(help = "Indice do historico (1-based)") int index) {
        var entry = service.getHistoryEntry(index);
        if (entry == null) return styled(RED, "Indice invalido.");
        return executeRequest(entry.request());
    }

    @ShellMethod(key = "clear-history", value = "Limpa o historico de requests")
    public String clearHistory() {
        service.clearHistory();
        return styled(GREEN, "Historico limpo.");
    }

    // ========== FORMATTING =================================================

    private String executeRequest(ApiRequest request) {
        try {
            System.out.println(styled(DIM, request.method() + " " + request.url() + "..."));
            var response = service.execute(request);
            return formatResponse(request, response);
        } catch (Exception e) {
            return styled(RED, "Erro: " + e.getMessage());
        }
    }

    private String formatResponse(ApiRequest req, ApiResponse res) {
        var sb = new StringBuilder();

        // Status line
        var statusStyle = res.isSuccess() ? BOLD_GREEN : (res.statusCode() < 500 ? BOLD_YELLOW : BOLD_RED);
        sb.append("\n  ");
        sb.append(styled(statusStyle, res.statusText()));
        sb.append(styled(DIM, "  |  "));
        sb.append(styled(CYAN, res.durationText()));
        sb.append(styled(DIM, "  |  "));
        sb.append(styled(CYAN, res.sizeText()));
        sb.append("\n");

        // Body preview (first 80 lines)
        if (!res.body().isBlank()) {
            sb.append("\n");
            var bodyText = res.isJson() ? jsonFormatter.prettify(res.body()) : res.body();
            var lines = bodyText.split("\n");
            int limit = Math.min(lines.length, 80);
            for (int i = 0; i < limit; i++) {
                sb.append("  ").append(lines[i]).append("\n");
            }
            if (lines.length > 80) {
                sb.append(styled(DIM, "  ... (" + (lines.length - 80) + " linhas omitidas. Use 'body' para ver tudo)\n"));
            }
        }

        return sb.toString();
    }

    private String formatFullResponse(ApiRequest req, ApiResponse res) {
        var sb = new StringBuilder();

        sb.append(styled(BOLD_CYAN, "\n  Request\n\n"));
        if (req != null) {
            sb.append(styled(BOLD, "  " + req.method() + " ")).append(styled(WHITE, req.url())).append("\n");
            if (!req.headers().isEmpty()) {
                for (var h : req.headers().entrySet()) {
                    sb.append(styled(DIM, "  " + h.getKey() + ": ")).append(h.getValue()).append("\n");
                }
            }
            if (req.body() != null && !req.body().isBlank()) {
                sb.append(styled(DIM, "\n  Body:\n"));
                sb.append("  ").append(req.body()).append("\n");
            }
        }

        sb.append(styled(BOLD_CYAN, "\n  Response\n\n"));
        var statusStyle = res.isSuccess() ? BOLD_GREEN : (res.statusCode() < 500 ? BOLD_YELLOW : BOLD_RED);
        sb.append("  ").append(styled(statusStyle, res.statusText()));
        sb.append(styled(DIM, "  |  "));
        sb.append(styled(CYAN, res.durationText()));
        sb.append(styled(DIM, "  |  "));
        sb.append(styled(CYAN, res.sizeText()));
        sb.append("\n\n");

        // Response headers
        for (var h : res.headers().entrySet()) {
            sb.append(styled(DIM, "  " + h.getKey() + ": ")).append(h.getValue()).append("\n");
        }

        // Body
        if (!res.body().isBlank()) {
            sb.append(styled(BOLD_CYAN, "\n  Body\n\n"));
            var bodyText = res.isJson() ? jsonFormatter.prettify(res.body()) : res.body();
            for (var line : bodyText.split("\n")) {
                sb.append("  ").append(line).append("\n");
            }
        }

        return sb.toString();
    }

    private String formatHeaders(String title, Map<String, String> headers) {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  " + title + "\n\n"));
        for (var h : headers.entrySet()) {
            sb.append(styled(GREEN, "  " + h.getKey())).append(styled(DIM, ": ")).append(h.getValue()).append("\n");
        }
        return sb.toString();
    }

    private String formatEnvironments(List<Environment> envs) {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  Ambientes\n\n"));

        var active = service.getActiveEnvironment();

        for (int i = 0; i < envs.size(); i++) {
            var env = envs.get(i);
            var isActive = active != null && active.name().equals(env.name());
            var marker = isActive ? styled(GREEN, " * ") : "   ";
            sb.append(marker);
            sb.append(styled(CYAN, String.format("%-3d ", i + 1)));
            sb.append(styled(isActive ? BOLD_GREEN : BOLD, env.name()));
            sb.append(styled(DIM, " (" + env.variables().size() + " variavel(is))"));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatEnvironmentDetail(Environment env) {
        var sb = new StringBuilder();
        var active = service.getActiveEnvironment();
        var isActive = active != null && active.name().equals(env.name());

        sb.append(styled(BOLD_CYAN, "\n  Ambiente: " + env.name()));
        if (isActive) sb.append(styled(GREEN, " (ativo)"));
        sb.append("\n\n");

        if (env.variables().isEmpty()) {
            sb.append(styled(DIM, "  Nenhuma variavel definida.\n"));
        } else {
            for (var v : env.variables().entrySet()) {
                sb.append(styled(YELLOW, "  {{" + v.getKey() + "}}"));
                sb.append(styled(DIM, " = "));
                sb.append(v.getValue()).append("\n");
            }
        }
        return sb.toString();
    }

    private String formatCollections(List<Collection> cols) {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  Colecoes\n\n"));

        for (int i = 0; i < cols.size(); i++) {
            var col = cols.get(i);
            sb.append(styled(CYAN, String.format("  %-3d ", i + 1)));
            sb.append(styled(BOLD, col.name()));
            sb.append(styled(DIM, " (" + col.requests().size() + " request(s))"));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatCollectionDetail(Collection col) {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  Colecao: " + col.name() + "\n\n"));

        if (col.requests().isEmpty()) {
            sb.append(styled(DIM, "  Nenhum request salvo.\n"));
            return sb.toString();
        }

        sb.append(styled(DIM, String.format("  %-4s %-8s %-30s %s%n", "#", "METODO", "NOME", "URL")));
        sb.append(styled(DIM, "  " + "-".repeat(90) + "\n"));

        for (int i = 0; i < col.requests().size(); i++) {
            var req = col.requests().get(i);
            var methodStyle = switch (req.method()) {
                case GET -> GREEN;
                case POST -> YELLOW;
                case PUT -> CYAN;
                case PATCH -> MAGENTA;
                case DELETE -> RED;
                default -> DIM;
            };
            sb.append(styled(CYAN, String.format("  %-4d ", i + 1)));
            sb.append(styled(methodStyle, String.format("%-8s ", req.method().name())));
            sb.append(styled(BOLD, String.format("%-30s ", truncate(req.name() != null ? req.name() : "-", 29))));
            sb.append(styled(DIM, truncate(req.url(), 50)));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatHistory(List<SavedRequest> entries) {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  Historico\n\n"));

        sb.append(styled(DIM, String.format("  %-4s %-8s %-6s %-45s %s%n",
                "#", "METODO", "STATUS", "URL", "TEMPO")));
        sb.append(styled(DIM, "  " + "-".repeat(90) + "\n"));

        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            var req = entry.request();

            var methodStyle = switch (req.method()) {
                case GET -> GREEN;
                case POST -> YELLOW;
                case PUT -> CYAN;
                case PATCH -> MAGENTA;
                case DELETE -> RED;
                default -> DIM;
            };

            var statusStyle = entry.statusCode() == 0 ? RED
                    : entry.statusCode() < 300 ? GREEN
                    : entry.statusCode() < 500 ? YELLOW : RED;

            sb.append(styled(CYAN, String.format("  %-4d ", i + 1)));
            sb.append(styled(methodStyle, String.format("%-8s ", req.method().name())));
            sb.append(styled(statusStyle, String.format("%-6s ", entry.statusCode() == 0 ? "ERR" : String.valueOf(entry.statusCode()))));
            sb.append(styled(DIM, String.format("%-45s ", truncate(req.url(), 44))));
            sb.append(styled(DIM, entry.duration() + "ms"));
            sb.append("\n");
        }

        sb.append("\n");
        sb.append(styled(DIM, "  Total: " + service.historySize() + " registro(s)\n"));
        return sb.toString();
    }

    // ========== HELPERS ====================================================

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

    /** Anexa query params a URL. Params no formato "key=value&key2=value2". */
    private String appendParams(String url, String params) {
        if (params == null || params.isBlank()) return url;
        var separator = url.contains("?") ? "&" : "?";
        return url + separator + params;
    }

    /** Resolve body (inline ou @arquivo) e retorna request com body setado. Retorna null se erro. */
    private ApiRequest resolveAndSetBody(ApiRequest req, String body) {
        if (body == null) return req;
        try {
            var resolved = service.resolveBody(body);
            return req.withBody(resolved);
        } catch (IOException e) {
            System.err.println("Erro ao ler body: " + e.getMessage());
            return null;
        }
    }

    private String formatRunAllResults(String colName, List<ApiService.RunResult> results) {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  Resultado: " + colName + "\n\n"));

        sb.append(styled(DIM, String.format("  %-4s %-8s %-6s %-30s %-40s %s%n",
                "#", "METODO", "STATUS", "NOME", "URL", "TEMPO")));
        sb.append(styled(DIM, "  " + "-".repeat(105) + "\n"));

        int passed = 0, failed = 0;
        long totalTime = 0;

        for (int i = 0; i < results.size(); i++) {
            var r = results.get(i);
            boolean success = r.error() == null && r.statusCode() >= 200 && r.statusCode() < 400;
            if (success) passed++;
            else failed++;
            totalTime += r.durationMs();

            var methodStyle = switch (r.method()) {
                case GET -> GREEN;
                case POST -> YELLOW;
                case PUT -> CYAN;
                case PATCH -> MAGENTA;
                case DELETE -> RED;
                default -> DIM;
            };

            var statusStyle = r.error() != null ? BOLD_RED
                    : r.statusCode() < 300 ? GREEN
                    : r.statusCode() < 500 ? YELLOW : RED;

            var statusText = r.error() != null ? "ERR" : String.valueOf(r.statusCode());

            sb.append(styled(success ? GREEN : RED, "  " + (success ? "+" : "x") + " "));
            sb.append(styled(CYAN, String.format("%-3d ", i + 1)));
            sb.append(styled(methodStyle, String.format("%-8s ", r.method().name())));
            sb.append(styled(statusStyle, String.format("%-6s ", statusText)));
            sb.append(styled(BOLD, String.format("%-30s ", truncate(r.name() != null ? r.name() : "-", 29))));
            sb.append(styled(DIM, String.format("%-40s ", truncate(r.url(), 39))));
            sb.append(styled(DIM, r.durationMs() + "ms"));
            sb.append("\n");

            if (r.error() != null) {
                sb.append(styled(RED, "         " + r.error())).append("\n");
            }
        }

        sb.append("\n");
        var summaryStyle = failed == 0 ? BOLD_GREEN : BOLD_YELLOW;
        sb.append(styled(summaryStyle, String.format("  %d passed, %d failed", passed, failed)));
        sb.append(styled(DIM, String.format("  |  total: %dms  |  %d request(s)", totalTime, results.size())));
        sb.append("\n");

        return sb.toString();
    }
}
