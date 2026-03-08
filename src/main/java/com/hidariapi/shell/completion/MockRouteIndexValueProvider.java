package com.hidariapi.shell.completion;

import com.hidariapi.service.MockServerService;
import org.springframework.shell.core.command.completion.CompletionContext;
import org.springframework.shell.core.command.completion.CompletionProposal;
import org.springframework.shell.core.command.completion.CompletionProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MockRouteIndexValueProvider implements CompletionProvider {

    private final MockServerService mockServerService;

    public MockRouteIndexValueProvider(MockServerService mockServerService) {
        this.mockServerService = mockServerService;
    }

    @Override
    public List<CompletionProposal> apply(CompletionContext completionContext) {
        var prefix = completionContext.currentWordUpToCursor();
        var result = new ArrayList<CompletionProposal>();
        int total = mockServerService.routeCount();
        for (int i = 1; i <= total; i++) {
            var value = String.valueOf(i);
            if (prefix == null || prefix.isBlank() || value.startsWith(prefix)) {
                result.add(new CompletionProposal(value));
            }
        }
        return result;
    }
}
