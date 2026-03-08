package com.hidariapi.util;

import com.hidariapi.model.ApiRequest;
import com.hidariapi.model.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Parser de comandos cURL para {@link ApiRequest}.
 * Suporta os flags mais comuns: -X, -H, -d/--data, --data-raw, -u.
 */
@Component
public class CurlParser {

    /**
     * Parseia um comando cURL e retorna um {@link ApiRequest}.
     *
     * @param curlCommand comando cURL completo (com ou sem "curl" no inicio)
     * @return ApiRequest equivalente
     * @throws IllegalArgumentException se o comando for invalido
     */
    public ApiRequest parse(String curlCommand) {
        if (curlCommand == null || curlCommand.isBlank()) {
            throw new IllegalArgumentException("Comando cURL vazio.");
        }

        var tokens = tokenize(curlCommand.trim());
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Comando cURL invalido.");
        }

        // Remover "curl" do inicio se presente
        int start = 0;
        if (tokens.getFirst().equalsIgnoreCase("curl")) {
            start = 1;
        }

        HttpMethod method = null;
        String url = null;
        var headers = new LinkedHashMap<String, String>();
        String body = null;

        for (int i = start; i < tokens.size(); i++) {
            var token = tokens.get(i);
            switch (token) {
                case "-X", "--request" -> {
                    if (i + 1 < tokens.size()) {
                        method = HttpMethod.fromString(tokens.get(++i));
                    }
                }
                case "-H", "--header" -> {
                    if (i + 1 < tokens.size()) {
                        var header = tokens.get(++i);
                        var colon = header.indexOf(':');
                        if (colon > 0) {
                            var key = header.substring(0, colon).trim();
                            var value = header.substring(colon + 1).trim();
                            headers.put(key, value);
                        }
                    }
                }
                case "-d", "--data", "--data-raw", "--data-binary" -> {
                    if (i + 1 < tokens.size()) {
                        body = tokens.get(++i);
                    }
                }
                case "-u", "--user" -> {
                    if (i + 1 < tokens.size()) {
                        var credentials = tokens.get(++i);
                        var encoded = java.util.Base64.getEncoder()
                                .encodeToString(credentials.getBytes());
                        headers.put("Authorization", "Basic " + encoded);
                    }
                }
                case "--compressed", "-s", "--silent", "-S", "--show-error",
                     "-k", "--insecure", "-L", "--location", "-v", "--verbose",
                     "-i", "--include" -> {
                    // Flags sem argumento — ignorar
                }
                case "--connect-timeout", "--max-time", "-o", "--output",
                     "--retry", "-w", "--write-out", "--cert", "--key" -> {
                    // Flags com argumento — pular o proximo token
                    if (i + 1 < tokens.size()) i++;
                }
                default -> {
                    // Se nao comeca com -, assume que e a URL
                    if (!token.startsWith("-") && url == null) {
                        url = token;
                    }
                }
            }
        }

        if (url == null) {
            throw new IllegalArgumentException("URL nao encontrada no comando cURL.");
        }

        // Se nao foi especificado metodo, inferir pelo body
        if (method == null) {
            method = body != null ? HttpMethod.POST : HttpMethod.GET;
        }

        return new ApiRequest(null, method, url, headers, body);
    }

    /**
     * Tokeniza o comando cURL respeitando aspas simples e duplas.
     * Trata corretamente: curl -H 'Content-Type: application/json' -d '{"key":"val"}'
     */
    private List<String> tokenize(String input) {
        var tokens = new ArrayList<String>();
        var current = new StringBuilder();
        char quote = 0;
        boolean escaped = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\' && quote != '\'') {
                escaped = true;
                continue;
            }

            if (quote != 0) {
                if (c == quote) {
                    quote = 0; // Fechar aspas
                } else {
                    current.append(c);
                }
                continue;
            }

            if (c == '\'' || c == '"') {
                quote = c; // Abrir aspas
                continue;
            }

            if (Character.isWhitespace(c)) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            current.append(c);
        }

        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }

        return tokens;
    }
}
