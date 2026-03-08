package com.hidariapi.shell.completion;

import com.hidariapi.model.HttpMethod;
import com.hidariapi.model.MockRoute;
import com.hidariapi.service.MockServerService;
import org.junit.jupiter.api.Test;
import org.springframework.shell.CompletionContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MockRoutePathValueProviderTest {

    @Test
    void completesDistinctPathsByPrefix() {
        var mockService = mock(MockServerService.class);
        when(mockService.listRoutes()).thenReturn(List.of(
                MockRoute.json(HttpMethod.GET, "/users", "[]"),
                MockRoute.json(HttpMethod.POST, "/users", "{}"),
                MockRoute.json(HttpMethod.GET, "/orders", "[]")
        ));

        var ctx = mock(CompletionContext.class);
        when(ctx.currentWordUpToCursor()).thenReturn("/u");

        var provider = new MockRoutePathValueProvider(mockService);
        var items = provider.complete(ctx);

        assertEquals(1, items.size());
        assertEquals("/users", items.getFirst().value());
    }
}
