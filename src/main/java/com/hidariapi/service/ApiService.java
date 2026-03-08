package com.hidariapi.service;

import com.hidariapi.model.*;
import com.hidariapi.store.CollectionStore;
import com.hidariapi.store.EnvironmentStore;
import com.hidariapi.store.HistoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        var resolved = resolveVariables(apiRequest);
        lastRequest = resolved;

        var builder = HttpRequest.newBuilder()
                .uri(URI.create(resolved.url()))
                .timeout(Duration.ofSeconds(timeoutSeconds));

        // Default headers
        for (var entry : defaultHeaders.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
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

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Resolve variaveis {{key}} usando o ambiente ativo. */
    private ApiRequest resolveVariables(ApiRequest request) {
        if (activeEnvironment == null) return request;

        var url = activeEnvironment.resolve(request.url());
        var body = activeEnvironment.resolve(request.body());
        var headers = new LinkedHashMap<String, String>();
        for (var entry : request.headers().entrySet()) {
            headers.put(entry.getKey(), activeEnvironment.resolve(entry.getValue()));
        }
        return new ApiRequest(request.name(), request.method(), url, headers, body);
    }
}
