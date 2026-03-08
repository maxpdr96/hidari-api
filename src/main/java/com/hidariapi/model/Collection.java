package com.hidariapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Colecao de requests agrupados (equivalente a uma Collection do Postman).
 *
 * @param name      nome da colecao
 * @param requests  lista de requests salvos
 * @param createdAt data de criacao
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Collection(
        String name,
        List<ApiRequest> requests,
        Instant createdAt
) {

    /** Cria colecao vazia. */
    public static Collection empty(String name) {
        return new Collection(name, new ArrayList<>(), Instant.now());
    }

    /** Retorna copia com request adicionado. */
    public Collection withRequest(ApiRequest request) {
        var reqs = new ArrayList<>(requests);
        reqs.add(request);
        return new Collection(name, reqs, createdAt);
    }

    /** Retorna copia sem o request no indice (0-based). */
    public Collection withoutRequest(int index) {
        var reqs = new ArrayList<>(requests);
        if (index >= 0 && index < reqs.size()) {
            reqs.remove(index);
        }
        return new Collection(name, reqs, createdAt);
    }
}
