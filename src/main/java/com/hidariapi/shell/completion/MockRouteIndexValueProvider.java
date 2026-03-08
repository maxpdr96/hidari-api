package com.hidariapi.shell.completion;

import com.hidariapi.service.MockServerService;
import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ValueProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MockRouteIndexValueProvider implements ValueProvider {

    private final MockServerService mockServerService;

    public MockRouteIndexValueProvider(MockServerService mockServerService) {
        this.mockServerService = mockServerService;
    }

    @Override
    public List<CompletionProposal> complete(CompletionContext completionContext) {
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
