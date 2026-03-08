package com.hidariapi.shell.completion;

import com.hidariapi.service.AliasService;
import org.springframework.shell.core.command.completion.CompletionContext;
import org.springframework.shell.core.command.completion.CompletionProposal;
import org.springframework.shell.core.command.completion.CompletionProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AliasNameValueProvider implements CompletionProvider {

    private final AliasService aliasService;

    public AliasNameValueProvider(AliasService aliasService) {
        this.aliasService = aliasService;
    }

    @Override
    public List<CompletionProposal> apply(CompletionContext completionContext) {
        var prefix = completionContext.currentWordUpToCursor();
        return aliasService.list().stream()
                .map(a -> a.name())
                .filter(name -> prefix == null || prefix.isBlank() || name.startsWith(prefix))
                .map(CompletionProposal::new)
                .toList();
    }
}
