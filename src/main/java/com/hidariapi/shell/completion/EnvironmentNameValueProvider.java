package com.hidariapi.shell.completion;

import com.hidariapi.service.ApiService;
import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ValueProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EnvironmentNameValueProvider implements ValueProvider {

    private final ApiService apiService;

    public EnvironmentNameValueProvider(ApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public List<CompletionProposal> complete(CompletionContext completionContext) {
        var prefix = completionContext.currentWordUpToCursor();
        return apiService.listEnvironments().stream()
                .map(env -> env.name())
                .filter(name -> prefix == null || prefix.isBlank() || name.startsWith(prefix))
                .map(CompletionProposal::new)
                .toList();
    }
}
