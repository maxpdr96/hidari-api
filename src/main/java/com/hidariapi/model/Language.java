package com.hidariapi.model;

/**
 * Supported display languages for the CLI.
 */
public enum Language {
    PT, EN;

    /** Case-insensitive parse, defaults to PT. */
    public static Language fromString(String s) {
        if (s == null) return PT;
        return switch (s.trim().toLowerCase()) {
            case "en", "eng", "english" -> EN;
            case "pt", "ptbr", "pt-br", "portugues", "portuguese" -> PT;
            default -> PT;
        };
    }
}
