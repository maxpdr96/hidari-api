package com.hidariapi.shell;

import com.hidariapi.model.Language;
import com.hidariapi.service.ConfigService;
import com.hidariapi.service.LanguageService;
import com.hidariapi.store.ConfigStore;
import org.junit.jupiter.api.Test;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.result.CommandNotFoundMessageProvider;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandSuggestionProviderTest {

    @Test
    void suggestsClosestCommandWhenTypoOccurs() {
        var lang = new LanguageService(new ConfigService(new ConfigStore()));
        lang.setCurrent(Language.EN);
        var provider = new CommandSuggestionProvider(lang);

        CommandRegistration getCmd = mock(CommandRegistration.class);
        when(getCmd.getCommand()).thenReturn("get");
        CommandRegistration mockListCmd = mock(CommandRegistration.class);
        when(mockListCmd.getCommand()).thenReturn("mock-list");

        var ctx = new CommandNotFoundMessageProvider.ProviderContext() {
            @Override
            public Throwable error() {
                return null;
            }

            @Override
            public List<String> commands() {
                return List.of("gret");
            }

            @Override
            public Map<String, CommandRegistration> registrations() {
                return Map.of("get", getCmd, "mock-list", mockListCmd);
            }

            @Override
            public String text() {
                return "gret https://x";
            }
        };

        var msg = provider.apply(ctx);
        assertTrue(msg.contains("Did you mean"));
        assertTrue(msg.contains("get"));
    }
}
