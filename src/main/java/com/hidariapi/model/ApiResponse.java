package com.hidariapi.model;

import java.time.Duration;
import java.util.Map;

/**
 * Resultado de uma requisicao HTTP.
 *
 * @param statusCode  codigo HTTP (200, 404, etc.)
 * @param headers     headers da resposta
 * @param body        corpo da resposta
 * @param duration    tempo da requisicao
 * @param contentType content-type retornado
 * @param size        tamanho do body em bytes
 */
public record ApiResponse(
        int statusCode,
        Map<String, String> headers,
        String body,
        Duration duration,
        String contentType,
        long size
) {

    /** Status formatado (e.g. "200 OK", "404 Not Found"). */
    public String statusText() {
        return switch (statusCode) {
            case 200 -> "200 OK";
            case 201 -> "201 Created";
            case 204 -> "204 No Content";
            case 301 -> "301 Moved Permanently";
            case 302 -> "302 Found";
            case 304 -> "304 Not Modified";
            case 400 -> "400 Bad Request";
            case 401 -> "401 Unauthorized";
            case 403 -> "403 Forbidden";
            case 404 -> "404 Not Found";
            case 405 -> "405 Method Not Allowed";
            case 408 -> "408 Request Timeout";
            case 409 -> "409 Conflict";
            case 422 -> "422 Unprocessable Entity";
            case 429 -> "429 Too Many Requests";
            case 500 -> "500 Internal Server Error";
            case 502 -> "502 Bad Gateway";
            case 503 -> "503 Service Unavailable";
            case 504 -> "504 Gateway Timeout";
            default -> String.valueOf(statusCode);
        };
    }

    /** Duração formatada (e.g. "123ms", "2.5s"). */
    public String durationText() {
        long ms = duration.toMillis();
        if (ms < 1000) return ms + "ms";
        return String.format("%.1fs", ms / 1000.0);
    }

    /** Tamanho formatado (e.g. "1.2 KB", "3.5 MB"). */
    public String sizeText() {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024));
    }

    /** Verifica se e JSON. */
    public boolean isJson() {
        return contentType != null && contentType.contains("json");
    }

    /** Verifica se e sucesso (2xx). */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
}
