package com.hidariapi.util;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves placeholders like {{token}} using a callback.
 */
public final class TemplateResolver {

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*}}");

    private TemplateResolver() {}

    public static String resolve(String text, Map<String, String> cache, Function<String, String> resolver) {
        if (text == null) return null;

        var matcher = TEMPLATE_PATTERN.matcher(text);
        var result = new StringBuffer();

        while (matcher.find()) {
            var expr = matcher.group(1).trim();
            var replacement = cache.computeIfAbsent(expr, resolver);
            if (replacement == null) {
                replacement = matcher.group(0);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }
}
