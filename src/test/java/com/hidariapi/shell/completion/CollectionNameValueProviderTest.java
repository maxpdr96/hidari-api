package com.hidariapi.shell.completion;

import com.hidariapi.model.Collection;
import com.hidariapi.service.ApiService;
import org.junit.jupiter.api.Test;
import org.springframework.shell.core.command.completion.CompletionContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CollectionNameValueProviderTest {

    @Test
    void completesByPrefix() {
        var api = mock(ApiService.class);
        when(api.listCollections()).thenReturn(List.of(Collection.empty("billing"), Collection.empty("users")));

        var ctx = mock(CompletionContext.class);
        when(ctx.currentWordUpToCursor()).thenReturn("b");

        var provider = new CollectionNameValueProvider(api);
        var items = provider.apply(ctx);

        assertEquals(1, items.size());
        assertEquals("billing", items.getFirst().value());
    }
}
