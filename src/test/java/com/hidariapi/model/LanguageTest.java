package com.hidariapi.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageTest {

    @Test
    void fromStringParsesKnownValues() {
        assertEquals(Language.EN, Language.fromString("en"));
        assertEquals(Language.EN, Language.fromString("ENG"));
        assertEquals(Language.PT, Language.fromString("pt-br"));
        assertEquals(Language.PT, Language.fromString("portuguese"));
    }

    @Test
    void fromStringFallsBackToPt() {
        assertEquals(Language.PT, Language.fromString(null));
        assertEquals(Language.PT, Language.fromString("unknown"));
    }
}
