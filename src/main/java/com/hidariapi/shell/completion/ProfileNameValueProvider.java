package com.hidariapi.shell.completion;

import com.hidariapi.service.ConfigService;
import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ValueProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProfileNameValueProvider implements ValueProvider {

    private final ConfigService configService;

    public ProfileNameValueProvider(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public List<CompletionProposal> complete(CompletionContext completionContext) {
        var prefix = completionContext.currentWordUpToCursor();
        return configService.listProfiles().keySet().stream()
                .filter(name -> prefix == null || prefix.isBlank() || name.startsWith(prefix))
                .map(CompletionProposal::new)
                .toList();
    }
}
