package com.hidariapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ambiente com variaveis substituiveis em URLs, headers e body.
 * Variaveis sao referenciadas como {{nome}} nos requests.
 *
 * @param name      nome do ambiente (e.g. "dev", "prod", "local")
 * @param variables mapa de variaveis (e.g. "base_url" -> "http://localhost:8080")
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Environment(
        String name,
        Map<String, String> variables
) {

    /** Cria ambiente vazio. */
    public static Environment empty(String name) {
        return new Environment(name, new LinkedHashMap<>());
    }

    /** Retorna copia com variavel adicionada/atualizada. */
    public Environment withVariable(String key, String value) {
        var vars = new LinkedHashMap<>(variables);
        vars.put(key, value);
        return new Environment(name, vars);
    }

    /** Retorna copia sem a variavel. */
    public Environment withoutVariable(String key) {
        var vars = new LinkedHashMap<>(variables);
        vars.remove(key);
        return new Environment(name, vars);
    }

    /** Substitui {{variaveis}} no texto usando este ambiente. */
    public String resolve(String text) {
        if (text == null) return null;
        var result = text;
        for (var entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}
