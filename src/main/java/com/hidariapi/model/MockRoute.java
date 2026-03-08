package com.hidariapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Define uma rota fake do mock server.
 * Cada rota responde a um metodo + path com status, headers e body fixos.
 *
 * @param method     metodo HTTP que ativa esta rota (GET, POST, etc.)
 * @param path       path da rota (e.g. "/api/users", "/api/users/{id}")
 * @param statusCode status HTTP da resposta (e.g. 200, 201, 404)
 * @param headers    headers da resposta
 * @param body       body da resposta
 * @param delay      delay em ms antes de responder (simular latencia)
 * @param description descricao curta da rota
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MockRoute(
        HttpMethod method,
        String path,
        int statusCode,
        Map<String, String> headers,
        String body,
        long delay,
        String description
) {

    /** Cria rota simples com body JSON e status 200. */
    public static MockRoute json(HttpMethod method, String path, String body) {
        var headers = new LinkedHashMap<String, String>();
        headers.put("Content-Type", "application/json");
        return new MockRoute(method, path, 200, headers, body, 0, null);
    }

    /** Cria rota com status customizado. */
    public static MockRoute withStatus(HttpMethod method, String path, int status, String body) {
        var headers = new LinkedHashMap<String, String>();
        headers.put("Content-Type", "application/json");
        return new MockRoute(method, path, status, headers, body, 0, null);
    }

    /** Retorna copia com header adicionado. */
    public MockRoute withHeader(String key, String value) {
        var h = new LinkedHashMap<>(headers);
        h.put(key, value);
        return new MockRoute(method, path, statusCode, h, body, delay, description);
    }

    /** Retorna copia com delay. */
    public MockRoute withDelay(long delayMs) {
        return new MockRoute(method, path, statusCode, headers, body, delayMs, description);
    }

    /** Retorna copia com descricao. */
    public MockRoute withDescription(String desc) {
        return new MockRoute(method, path, statusCode, headers, body, delay, desc);
    }

    /** Chave unica: METHOD + path. */
    public String routeKey() {
        return method.name() + " " + path;
    }

    /**
     * Verifica se o path do request bate com esta rota.
     * Suporta path params simples: /api/users/{id} bate com /api/users/123.
     */
    public boolean matches(String requestMethod, String requestPath) {
        if (!method.name().equalsIgnoreCase(requestMethod)) return false;

        var routeParts = path.split("/");
        var reqParts = requestPath.split("\\?")[0].split("/"); // Ignorar query params

        if (routeParts.length != reqParts.length) return false;

        for (int i = 0; i < routeParts.length; i++) {
            var rp = routeParts[i];
            if (rp.startsWith("{") && rp.endsWith("}")) continue; // Wildcard
            if (!rp.equals(reqParts[i])) return false;
        }
        return true;
    }
}
