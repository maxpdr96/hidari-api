package com.hidariapi.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

/**
 * Formata JSON para exibicao bonita no terminal.
 */
@Component
public class JsonFormatter {

    private final ObjectMapper mapper;

    public JsonFormatter() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /** Tenta formatar JSON bonito. Se falhar, retorna o texto original. */
    public String prettify(String json) {
        if (json == null || json.isBlank()) return "";
        try {
            var tree = mapper.readTree(json);
            return mapper.writeValueAsString(tree);
        } catch (Exception e) {
            return json;
        }
    }

    /** Verifica se o texto e JSON valido. */
    public boolean isValidJson(String text) {
        if (text == null || text.isBlank()) return false;
        try {
            mapper.readTree(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
