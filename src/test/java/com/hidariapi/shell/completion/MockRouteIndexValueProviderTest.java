package com.hidariapi.shell.completion;

import com.hidariapi.service.MockServerService;
import org.junit.jupiter.api.Test;
import org.springframework.shell.core.command.completion.CompletionContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MockRouteIndexValueProviderTest {

    @Test
    void completesIndicesByPrefix() {
        var mockService = mock(MockServerService.class);
        when(mockService.routeCount()).thenReturn(12);

        var ctx = mock(CompletionContext.class);
        when(ctx.currentWordUpToCursor()).thenReturn("1");

        var provider = new MockRouteIndexValueProvider(mockService);
        var items = provider.apply(ctx);

        assertEquals(4, items.size());
        assertEquals("1", items.get(0).value());
        assertEquals("12", items.get(3).value());
    }
}
