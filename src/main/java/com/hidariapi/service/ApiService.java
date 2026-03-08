package com.hidariapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hidariapi.model.*;
import com.hidariapi.util.BrazilianDataGenerator;
import com.hidariapi.store.CollectionStore;
import com.hidariapi.store.EnvironmentStore;
import com.hidariapi.store.HistoryStore;
import com.hidariapi.util.TemplateResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Servico principal que executa requests HTTP e gerencia sessao.
 */
@Service
public class ApiService {

    private final HttpClient httpClient;
    private final CollectionStore collectionStore;
    private final HistoryStore historyStore;
    private final EnvironmentStore environmentStore;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Headers padrao aplicados a todo request. */
    private final Map<String, String> defaultHeaders = new LinkedHashMap<>();

    /** Ambiente ativo (pode ser null). */
    private Environment activeEnvironment;

    /** Ultima resposta recebida. */
    private ApiResponse lastResponse;

    /** Ultimo request enviado. */
    private ApiRequest lastRequest;

    public ApiService(
            CollectionStore collectionStore,
            HistoryStore historyStore,
            EnvironmentStore environmentStore,
            @Value("${hidariapi.timeout-seconds:30}") int timeoutSeconds) {
        this.collectionStore = collectionStore;
        this.historyStore = historyStore;
        this.environmentStore = environmentStore;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    // ── Execução de requests ─────────────────────────────────────────────────

    /** Executa um request HTTP. */
    public ApiResponse execute(ApiRequest apiRequest) throws IOException, InterruptedException {
        var context = new TemplateContext(activeEnvironment, lastResponse);
        var resolved = resolveVariables(apiRequest, context);
        lastRequest = resolved;

        var builder = HttpRequest.newBuilder()
                .uri(URI.create(resolved.url()))
                .timeout(Duration.ofSeconds(timeoutSeconds));

        // Default headers
        for (var entry : defaultHeaders.entrySet()) {
            var value = resolveText(entry.getValue(), context);
            builder.header(entry.getKey(), value);
        }

        // Request headers
        for (var entry : resolved.headers().entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }

        // Method + body
        var bodyPublisher = resolved.body() != null && !resolved.body().isBlank()
                ? HttpRequest.BodyPublishers.ofString(resolved.body())
                : HttpRequest.BodyPublishers.noBody();

        builder.method(resolved.method().name(), bodyPublisher);

        var start = System.nanoTime();
        var response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        var duration = Duration.ofNanos(System.nanoTime() - start);

        // Parse response headers
        var responseHeaders = new LinkedHashMap<String, String>();
        response.headers().map().forEach((k, v) -> responseHeaders.put(k, String.join(", ", v)));

        var contentType = responseHeaders.getOrDefault("content-type", "");
        var body = response.body() != null ? response.body() : "";

        lastResponse = new ApiResponse(
                response.statusCode(),
                responseHeaders,
                body,
                duration,
                contentType,
                body.length()
        );

        // Salvar no historico
        historyStore.add(SavedRequest.from(apiRequest, lastResponse));

        return lastResponse;
    }

    /** Atalho para GET. */
    public ApiResponse get(String url) throws IOException, InterruptedException {
        return execute(ApiRequest.of(null, HttpMethod.GET, url));
    }

    /** Atalho para POST com body. */
    public ApiResponse post(String url, String body) throws IOException, InterruptedException {
        var req = ApiRequest.of(null, HttpMethod.POST, url)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
        return execute(req);
    }

    /** Atalho para PUT com body. */
    public ApiResponse put(String url, String body) throws IOException, InterruptedException {
        var req = ApiRequest.of(null, HttpMethod.PUT, url)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
        return execute(req);
    }

    /** Atalho para PATCH com body. */
    public ApiResponse patch(String url, String body) throws IOException, InterruptedException {
        var req = ApiRequest.of(null, HttpMethod.PATCH, url)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
        return execute(req);
    }

    /** Atalho para DELETE. */
    public ApiResponse delete(String url) throws IOException, InterruptedException {
        return execute(ApiRequest.of(null, HttpMethod.DELETE, url));
    }

    // ── Default headers ──────────────────────────────────────────────────────

    /** Define header padrao. */
    public void setDefaultHeader(String key, String value) {
        defaultHeaders.put(key, value);
    }

    /** Remove header padrao. */
    public void removeDefaultHeader(String key) {
        defaultHeaders.remove(key);
    }

    /** Retorna headers padrao. */
    public Map<String, String> getDefaultHeaders() {
        return new LinkedHashMap<>(defaultHeaders);
    }

    /** Limpa todos os headers padrao. */
    public void clearDefaultHeaders() {
        defaultHeaders.clear();
    }

    // ── Ambientes ────────────────────────────────────────────────────────────

    /** Define o ambiente ativo. */
    public void setActiveEnvironment(String name) {
        activeEnvironment = environmentStore.get(name).orElse(null);
    }

    /** Remove o ambiente ativo. */
    public void clearActiveEnvironment() {
        activeEnvironment = null;
    }

    /** Retorna o ambiente ativo. */
    public Environment getActiveEnvironment() {
        return activeEnvironment;
    }

    /** Cria ou atualiza ambiente. */
    public void saveEnvironment(Environment env) {
        environmentStore.save(env);
    }

    /** Remove um ambiente. */
    public boolean removeEnvironment(String name) {
        if (activeEnvironment != null && activeEnvironment.name().equals(name)) {
            activeEnvironment = null;
        }
        return environmentStore.remove(name);
    }

    /** Lista ambientes. */
    public List<Environment> listEnvironments() {
        return environmentStore.list();
    }

    /** Retorna ambiente pelo nome. */
    public Optional<Environment> getEnvironment(String name) {
        return environmentStore.get(name);
    }

    // ── Colecoes ─────────────────────────────────────────────────────────────

    public void saveCollection(Collection collection) {
        collectionStore.save(collection);
    }

    public boolean removeCollection(String name) {
        return collectionStore.remove(name);
    }

    public List<Collection> listCollections() {
        return collectionStore.list();
    }

    public Optional<Collection> getCollection(String name) {
        return collectionStore.get(name);
    }

    public Optional<Collection> getCollectionByIndex(int index) {
        return collectionStore.getByIndex(index);
    }

    // ── Historico ────────────────────────────────────────────────────────────

    public List<SavedRequest> getHistory(int limit) {
        return historyStore.last(limit);
    }

    public SavedRequest getHistoryEntry(int index) {
        return historyStore.get(index);
    }

    public void clearHistory() {
        historyStore.clear();
    }

    public int historySize() {
        return historyStore.size();
    }

    // ── Sessão ───────────────────────────────────────────────────────────────

    public ApiResponse getLastResponse() {
        return lastResponse;
    }

    public ApiRequest getLastRequest() {
        return lastRequest;
    }

    // ── Body de arquivo ──────────────────────────────────────────────────────

    /**
     * Resolve body: se comecar com @, le o conteudo do arquivo.
     * Ex: "@/home/user/payload.json" le o arquivo e retorna o conteudo.
     */
    public String resolveBody(String body) throws IOException {
        if (body == null) return null;
        if (body.startsWith("@")) {
            var path = Path.of(body.substring(1));
            if (!Files.exists(path)) {
                throw new IOException("Arquivo nao encontrado: " + path);
            }
            return Files.readString(path);
        }
        return body;
    }

    // ── Run All ──────────────────────────────────────────────────────────────

    /** Resultado de execucao de um request da colecao. */
    public record RunResult(String name, HttpMethod method, String url, int statusCode, long durationMs, String error) {}

    /** Executa todos os requests de uma colecao e retorna resultados. */
    public List<RunResult> runCollection(String collectionName) {
        var col = collectionStore.get(collectionName);
        if (col.isEmpty()) return List.of();

        var results = new ArrayList<RunResult>();
        for (var req : col.get().requests()) {
            try {
                var response = execute(req);
                results.add(new RunResult(
                        req.name(), req.method(), req.url(),
                        response.statusCode(), response.duration().toMillis(), null));
            } catch (Exception e) {
                results.add(new RunResult(
                        req.name(), req.method(), req.url(),
                        0, 0, e.getMessage()));
            }
        }
        return results;
    }

    // ── Save Response ────────────────────────────────────────────────────────

    /** Salva o body da ultima resposta em um arquivo. */
    public void saveResponseToFile(String filePath) throws IOException {
        if (lastResponse == null) {
            throw new IOException("Nenhuma resposta para salvar.");
        }
        var path = Path.of(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, lastResponse.body());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Resolve variaveis de ambiente e runtime em URL/headers/body. */
    private ApiRequest resolveVariables(ApiRequest request, TemplateContext context) {
        var url = resolveText(request.url(), context);
        var body = resolveText(request.body(), context);
        var headers = new LinkedHashMap<String, String>();
        for (var entry : request.headers().entrySet()) {
            headers.put(entry.getKey(), resolveText(entry.getValue(), context));
        }
        return new ApiRequest(request.name(), request.method(), url, headers, body);
    }

    private String resolveText(String text, TemplateContext context) {
        return TemplateResolver.resolve(text, context.cache(), expr -> resolveExpression(expr, context));
    }

    private String resolveExpression(String expr, TemplateContext context) {
        return switch (expr) {
            case "$timestamp" -> String.valueOf(context.nowMillis());
            case "$isoTimestamp" -> context.now().toString();
            case "$uuid" -> UUID.randomUUID().toString();
            case "$cpf", "faker.cpf" -> BrazilianDataGenerator.randomCpf();
            case "$cnpj", "faker.cnpj" -> BrazilianDataGenerator.randomCnpj();
            case "$cep", "faker.cep" -> BrazilianDataGenerator.randomCep();
            case "$phoneBr", "faker.phone_br" -> BrazilianDataGenerator.randomPhoneBr();
            case "$fullNameBr", "faker.full_name_br" -> BrazilianDataGenerator.randomFullNameBr();
            case "$addressBr", "faker.address_br" -> BrazilianDataGenerator.randomAddressBr();
            case "last.status" -> context.lastResponse() != null ? String.valueOf(context.lastResponse().statusCode()) : null;
            case "last.body" -> context.lastResponse() != null ? context.lastResponse().body() : null;
            default -> resolveStructuredExpression(expr, context);
        };
    }

    private String resolveStructuredExpression(String expr, TemplateContext context) {
        if (expr.startsWith("env.")) {
            return context.environment() != null ? context.environment().variables().get(expr.substring("env.".length())) : null;
        }

        if (context.environment() != null && context.environment().variables().containsKey(expr)) {
            return context.environment().variables().get(expr);
        }

        if (expr.startsWith("last.header.") && context.lastResponse() != null) {
            var target = expr.substring("last.header.".length());
            for (var h : context.lastResponse().headers().entrySet()) {
                if (h.getKey().equalsIgnoreCase(target)) {
                    return h.getValue();
                }
            }
            return null;
        }

        if (expr.startsWith("last.body.") && context.lastResponse() != null) {
            var jsonPath = expr.substring("last.body.".length());
            return readJsonPath(context.lastResponse().body(), jsonPath);
        }

        return null;
    }

    private String readJsonPath(String json, String path) {
        if (json == null || json.isBlank() || path == null || path.isBlank()) return null;
        try {
            var node = objectMapper.readTree(json);
            for (var segment : path.split("\\.")) {
                if (segment.isBlank() || node == null) return null;
                node = navigateSegment(node, segment);
            }
            if (node == null || node.isMissingNode() || node.isNull()) return null;
            return node.isValueNode() ? node.asText() : node.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private com.fasterxml.jackson.databind.JsonNode navigateSegment(com.fasterxml.jackson.databind.JsonNode node, String segment) {
        int cursor = 0;
        if (!segment.startsWith("[")) {
            int bracket = segment.indexOf('[');
            String field = bracket >= 0 ? segment.substring(0, bracket) : segment;
            node = node.get(field);
            if (node == null) return null;
            cursor = field.length();
        }

        while (cursor < segment.length()) {
            if (segment.charAt(cursor) != '[') return null;
            int end = segment.indexOf(']', cursor);
            if (end < 0) return null;
            int index = Integer.parseInt(segment.substring(cursor + 1, end));
            node = node.get(index);
            if (node == null) return null;
            cursor = end + 1;
        }
        return node;
    }

    private record TemplateContext(
            Environment environment,
            ApiResponse lastResponse,
            Instant now,
            long nowMillis,
            Map<String, String> cache
    ) {
        private TemplateContext(Environment environment, ApiResponse lastResponse) {
            this(environment, lastResponse, Instant.now(), System.currentTimeMillis(), new HashMap<>());
        }
    }
}
