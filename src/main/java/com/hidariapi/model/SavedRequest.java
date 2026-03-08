package com.hidariapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Request salvo no historico com metadados de execucao.
 *
 * @param request    a requisicao original
 * @param statusCode codigo de resposta (0 se erro)
 * @param duration   duracao em ms
 * @param timestamp  quando foi executado
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SavedRequest(
        ApiRequest request,
        int statusCode,
        long duration,
        Instant timestamp
) {

    /** Cria a partir de request e response. */
    public static SavedRequest from(ApiRequest req, ApiResponse res) {
        return new SavedRequest(req, res.statusCode(), res.duration().toMillis(), Instant.now());
    }

    /** Cria para request com erro. */
    public static SavedRequest error(ApiRequest req) {
        return new SavedRequest(req, 0, 0, Instant.now());
    }
}
