package com.hidariapi.shell;

import com.hidariapi.model.Language;
import com.hidariapi.service.ConfigService;
import com.hidariapi.service.LanguageService;
import com.hidariapi.store.ConfigStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandSuggestionProviderTest {

    @Test
    void suggestsClosestCommandWhenTypoOccurs() {
        var lang = new LanguageService(new ConfigService(new ConfigStore()));
        lang.setCurrent(Language.EN);
        var provider = new CommandSuggestionProvider(lang);

        var msg = provider.suggest("gret https://x", List.of("get", "mock-list"));
        assertTrue(msg.contains("Did you mean"));
        assertTrue(msg.contains("get"));
    }
}
