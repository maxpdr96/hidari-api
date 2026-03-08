package com.hidariapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * User-defined command alias.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CommandAlias(
        String name,
        String command,
        Instant createdAt
) {
    public static CommandAlias of(String name, String command) {
        return new CommandAlias(name, command, Instant.now());
    }
}
