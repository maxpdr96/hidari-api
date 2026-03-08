package com.hidariapi.shell.completion;

import com.hidariapi.model.Environment;
import com.hidariapi.service.ApiService;
import org.junit.jupiter.api.Test;
import org.springframework.shell.core.command.completion.CompletionContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EnvironmentNameValueProviderTest {

    @Test
    void completesByPrefix() {
        var api = mock(ApiService.class);
        when(api.listEnvironments()).thenReturn(List.of(Environment.empty("dev"), Environment.empty("prod")));

        var ctx = mock(CompletionContext.class);
        when(ctx.currentWordUpToCursor()).thenReturn("d");

        var provider = new EnvironmentNameValueProvider(api);
        var items = provider.apply(ctx);

        assertEquals(1, items.size());
        assertEquals("dev", items.getFirst().value());
    }
}
