package com.hidariapi.shell;

import com.hidariapi.service.AliasService;
import com.hidariapi.service.LanguageService;
import com.hidariapi.util.CommandLineTokenizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.shell.core.InputReader;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandExecutor;
import org.springframework.shell.core.command.CommandParser;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

@Component
public class AliasCommands extends LocalizedSupport {

    private final AliasService aliasService;
    private final ObjectProvider<CommandParser> commandParserProvider;
    private final ObjectProvider<CommandRegistry> commandRegistryProvider;

    public AliasCommands(
            AliasService aliasService,
            ObjectProvider<CommandParser> commandParserProvider,
            ObjectProvider<CommandRegistry> commandRegistryProvider,
            LanguageService lang) {
        super(lang);
        this.aliasService = aliasService;
        this.commandParserProvider = commandParserProvider;
        this.commandRegistryProvider = commandRegistryProvider;
    }

    @Command(name = "alias-set", description = "Cria/atualiza alias customizado")
    public String aliasSet(
            @Option(description = "Nome do alias", required = true) String name,
            @Option(description = "Comando alvo (use aspas)", required = true) String command) {
        if (name == null || name.isBlank()) return styled(RED, t("Nome do alias invalido.", "Invalid alias name."));
        if (command == null || command.isBlank()) return styled(RED, t("Comando alvo invalido.", "Invalid target command."));
        aliasService.save(name, command);
        return styled(GREEN, t("Alias salvo: ", "Alias saved: ")) + styled(BOLD, name)
                + styled(DIM, " -> " + command);
    }

    @Command(name = "alias-rm", description = "Remove alias")
    public String aliasRm(@Option(description = "Nome do alias", required = true) String name) {
        if (aliasService.remove(name)) {
            return styled(GREEN, t("Alias removido: ", "Alias removed: ") + name);
        }
        return styled(RED, t("Alias nao encontrado: ", "Alias not found: ") + name);
    }

    @Command(name = "aliases", description = "Lista aliases")
    public String aliases() {
        var list = aliasService.list();
        if (list.isEmpty()) return styled(DIM, t("Nenhum alias configurado.", "No aliases configured."));

        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  " + t("Aliases", "Aliases") + "\n\n"));
        for (var a : list) {
            sb.append(styled(CYAN, "  " + a.name()));
            sb.append(styled(DIM, " -> "));
            sb.append(styled(WHITE, a.command()));
            sb.append("\n");
        }
        return sb.toString();
    }

    @Command(name = "alias-run", alias = "a", description = "Executa um alias")
    public String aliasRun(
            @Option(description = "Nome do alias", required = true) String name,
            @Option(longName = "args", defaultValue = "", description = "Argumentos extras") String args) {
        var aliasOpt = aliasService.get(name);
        if (aliasOpt.isEmpty()) return styled(RED, t("Alias nao encontrado: ", "Alias not found: ") + name);

        String raw = aliasOpt.get().command();
        if (args != null && !args.isBlank()) raw = raw + " " + args;
        if (raw.isBlank()) {
            return styled(RED, t("Alias vazio.", "Empty alias."));
        }

        try {
            var tokens = new ArrayList<>(CommandLineTokenizer.tokenize(raw));
            if (tokens.isEmpty()) return styled(RED, t("Alias vazio.", "Empty alias."));

            var commandParser = commandParserProvider.getObject();
            var commandRegistry = commandRegistryProvider.getObject();
            var parsedInput = commandParser.parse(String.join(" ", tokens));
            var outputBuffer = new StringWriter();
            InputReader inputReader = new InputReader() {};
            var context = new CommandContext(parsedInput, commandRegistry, new PrintWriter(outputBuffer), inputReader);
            var exitStatus = new CommandExecutor(commandRegistry).execute(context);
            var out = outputBuffer.toString().trim();
            if (!out.isBlank()) {
                return out;
            }
            return exitStatus.code() == 0
                    ? styled(GREEN, t("Alias executado.", "Alias executed."))
                    : styled(RED, t("Alias falhou.", "Alias failed."));
        } catch (Exception e) {
            return styled(RED, t("Erro ao executar alias: ", "Error executing alias: ") + e.getMessage());
        }
    }
}
