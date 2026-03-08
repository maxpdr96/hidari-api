package com.hidariapi.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandLineTokenizerTest {

    @Test
    void tokenizesQuotedAndUnquotedSegments() {
        var tokens = CommandLineTokenizer.tokenize("post https://a.com --body '{\"x\":1}' --header \"X-Test:ok\"");
        assertEquals(6, tokens.size());
        assertEquals("post", tokens.get(0));
        assertEquals("https://a.com", tokens.get(1));
        assertEquals("--body", tokens.get(2));
        assertEquals("{\"x\":1}", tokens.get(3));
        assertEquals("--header", tokens.get(4));
        assertEquals("X-Test:ok", tokens.get(5));
    }
}
