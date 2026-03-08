package com.hidariapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Representa uma requisicao HTTP salva ou pronta para envio.
 *
 * @param name    nome da requisicao (para colecoes)
 * @param method  metodo HTTP
 * @param url     URL completa
 * @param headers mapa de headers
 * @param body    corpo da requisicao (pode ser null)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiRequest(
        String name,
        HttpMethod method,
        String url,
        Map<String, String> headers,
        String body
) {

    /** Cria request basico sem body. */
    public static ApiRequest of(String name, HttpMethod method, String url) {
        return new ApiRequest(name, method, url, new LinkedHashMap<>(), null);
    }

    /** Retorna copia com header adicionado. */
    public ApiRequest withHeader(String key, String value) {
        var h = new LinkedHashMap<>(headers);
        h.put(key, value);
        return new ApiRequest(name, method, url, h, body);
    }

    /** Retorna copia com body. */
    public ApiRequest withBody(String body) {
        return new ApiRequest(name, method, url, headers, body);
    }

    /** Retorna copia com nome diferente. */
    public ApiRequest withName(String name) {
        return new ApiRequest(name, method, url, headers, body);
    }

    /** Gera comando cURL equivalente. */
    public String toCurl() {
        var sb = new StringBuilder("curl");
        if (method != HttpMethod.GET) {
            sb.append(" -X ").append(method.name());
        }
        for (var entry : headers.entrySet()) {
            sb.append(" -H '").append(entry.getKey()).append(": ").append(entry.getValue()).append("'");
        }
        if (body != null && !body.isBlank()) {
            sb.append(" -d '").append(body.replace("'", "'\\''")).append("'");
        }
        sb.append(" '").append(url).append("'");
        return sb.toString();
    }
}
