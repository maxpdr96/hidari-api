package com.hidariapi.shell.completion;

import com.hidariapi.service.MockServerService;
import org.springframework.shell.core.command.completion.CompletionContext;
import org.springframework.shell.core.command.completion.CompletionProposal;
import org.springframework.shell.core.command.completion.CompletionProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockRoutePathValueProvider implements CompletionProvider {

    private final MockServerService mockServerService;

    public MockRoutePathValueProvider(MockServerService mockServerService) {
        this.mockServerService = mockServerService;
    }

    @Override
    public List<CompletionProposal> apply(CompletionContext completionContext) {
        var prefix = completionContext.currentWordUpToCursor();
        return mockServerService.listRoutes().stream()
                .map(route -> route.path())
                .distinct()
                .filter(path -> prefix == null || prefix.isBlank() || path.startsWith(prefix))
                .map(CompletionProposal::new)
                .toList();
    }
}
