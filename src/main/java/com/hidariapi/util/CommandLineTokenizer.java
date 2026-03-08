package com.hidariapi.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a command line preserving quoted segments.
 */
public final class CommandLineTokenizer {

    private CommandLineTokenizer() {}

    public static List<String> tokenize(String input) {
        var tokens = new ArrayList<String>();
        if (input == null || input.isBlank()) return tokens;

        var current = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
                continue;
            }
            if (c == '"' && !inSingle) {
                inDouble = !inDouble;
                continue;
            }

            if (Character.isWhitespace(c) && !inSingle && !inDouble) {
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
