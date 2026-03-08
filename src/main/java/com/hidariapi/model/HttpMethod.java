package com.hidariapi.model;

/**
 * Metodos HTTP suportados.
 */
public enum HttpMethod {
    GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS;

    /** Parse case-insensitive, fallback para GET. */
    public static HttpMethod fromString(String s) {
        try {
            return valueOf(s.toUpperCase());
        } catch (Exception e) {
            return GET;
        }
    }
}
