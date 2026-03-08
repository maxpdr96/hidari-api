package com.hidariapi.shell;

import com.hidariapi.service.AliasService;
import com.hidariapi.service.LanguageService;
import com.hidariapi.shell.completion.AliasNameValueProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.shell.Input;
import org.springframework.shell.Shell;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class AliasCommands extends LocalizedSupport {

    private final AliasService aliasService;
    private final ObjectProvider<Shell> shellProvider;

    public AliasCommands(AliasService aliasService, ObjectProvider<Shell> shellProvider, LanguageService lang) {
        super(lang);
        this.aliasService = aliasService;
        this.shellProvider = shellProvider;
    }

    @ShellMethod(key = "alias-set", value = "Cria/atualiza alias customizado")
    public String aliasSet(
            @ShellOption(help = "Nome do alias") String name,
            @ShellOption(help = "Comando alvo (use aspas)") String command) {
        if (name == null || name.isBlank()) return styled(RED, t("Nome do alias invalido.", "Invalid alias name."));
        if (command == null || command.isBlank()) return styled(RED, t("Comando alvo invalido.", "Invalid target command."));
        aliasService.save(name, command);
        return styled(GREEN, t("Alias salvo: ", "Alias saved: ")) + styled(BOLD, name)
                + styled(DIM, " -> " + command);
    }

    @ShellMethod(key = "alias-rm", value = "Remove alias")
    public String aliasRm(
            @ShellOption(help = "Nome do alias", valueProvider = AliasNameValueProvider.class) String name) {
        if (aliasService.remove(name)) {
            return styled(GREEN, t("Alias removido: ", "Alias removed: ") + name);
        }
        return styled(RED, t("Alias nao encontrado: ", "Alias not found: ") + name);
    }

    @ShellMethod(key = "aliases", value = "Lista aliases")
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

    @ShellMethod(key = {"alias-run", "a"}, value = "Executa um alias")
    public String aliasRun(
            @ShellOption(help = "Nome do alias", valueProvider = AliasNameValueProvider.class) String name,
            @ShellOption(value = "--args", defaultValue = ShellOption.NULL, help = "Argumentos extras") String args) {
        var aliasOpt = aliasService.get(name);
        if (aliasOpt.isEmpty()) return styled(RED, t("Alias nao encontrado: ", "Alias not found: ") + name);

        String raw = aliasOpt.get().command();
        if (args != null && !args.isBlank()) raw = raw + " " + args;
        if (raw.isBlank()) {
            return styled(RED, t("Alias vazio.", "Empty alias."));
        }

        try {
            Object result = evaluate(raw);
            if (result == null) return styled(GREEN, t("Alias executado.", "Alias executed."));
            return result.toString();
        } catch (Exception e) {
            return styled(RED, t("Erro ao executar alias: ", "Error executing alias: ") + e.getMessage());
        }
    }

    private Object evaluate(String raw) throws Exception {
        var shell = shellProvider.getObject();
        var method = Shell.class.getDeclaredMethod("evaluate", Input.class);
        method.setAccessible(true);
        return method.invoke(shell, (Input) () -> raw);
    }
}
