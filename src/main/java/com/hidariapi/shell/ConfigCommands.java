package com.hidariapi.shell;

import com.hidariapi.service.ConfigService;
import com.hidariapi.service.LanguageService;
import com.hidariapi.shell.completion.ProfileNameValueProvider;
import com.hidariapi.shell.completion.ShortcutNameValueProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.shell.Input;
import org.springframework.shell.Shell;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class ConfigCommands extends LocalizedSupport {

    private final ConfigService configService;
    private final ObjectProvider<Shell> shellProvider;

    public ConfigCommands(ConfigService configService, ObjectProvider<Shell> shellProvider, LanguageService lang) {
        super(lang);
        this.configService = configService;
        this.shellProvider = shellProvider;
    }

    @ShellMethod(key = "config-set", value = "Define valor de configuracao")
    public String configSet(
            @ShellOption(help = "Chave (ex: language, request.default-parallel, profile.dev.base-url)") String key,
            @ShellOption(help = "Valor") String value) {
        boolean ok = configService.setByKey(key, value);
        if (!ok) return styled(RED, t("Chave invalida: ", "Invalid key: ") + key);
        return styled(GREEN, t("Configuracao atualizada.", "Configuration updated."));
    }

    @ShellMethod(key = "config-get", value = "Le valor de configuracao")
    public String configGet(@ShellOption(help = "Chave") String key) {
        var value = configService.getByKey(key);
        if (value == null) return styled(RED, t("Chave nao encontrada: ", "Key not found: ") + key);
        return styled(CYAN, key) + styled(DIM, " = ") + styled(WHITE, value);
    }

    @ShellMethod(key = "config-list", value = "Lista configuracoes")
    public String configList() {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  " + t("Configuracoes", "Configuration") + "\n\n"));
        configService.listFlat().forEach((k, v) -> {
            sb.append(styled(CYAN, "  " + k));
            sb.append(styled(DIM, " = "));
            sb.append(v == null || v.isBlank() ? styled(DIM, "<empty>") : styled(WHITE, v));
            sb.append("\n");
        });
        return sb.toString();
    }

    @ShellMethod(key = "profile-use", value = "Ativa profile de configuracao")
    public String profileUse(@ShellOption(help = "Nome do profile", valueProvider = ProfileNameValueProvider.class) String name) {
        configService.useProfile(name);
        return styled(GREEN, t("Profile ativo: ", "Active profile: ") + configService.getActiveProfile());
    }

    @ShellMethod(key = "profile-set-base-url", value = "Define base_url de um profile")
    public String profileSetBaseUrl(
            @ShellOption(help = "Nome do profile", valueProvider = ProfileNameValueProvider.class) String profile,
            @ShellOption(help = "Base URL") String baseUrl) {
        configService.setProfileBaseUrl(profile, baseUrl);
        return styled(GREEN, t("Base URL definida para ", "Base URL set for ") + profile + ": " + baseUrl);
    }

    @ShellMethod(key = "profile-list", value = "Lista profiles de configuracao")
    public String profileList() {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  " + t("Profiles", "Profiles") + "\n\n"));
        configService.listProfiles().forEach((name, cfg) -> {
            boolean active = name.equals(configService.getActiveProfile());
            sb.append(styled(active ? GREEN : CYAN, "  " + (active ? "*" : "-") + " " + name));
            sb.append(styled(DIM, "  base_url=" + (cfg.baseUrl() != null ? cfg.baseUrl() : ""))).append("\n");
        });
        return sb.toString();
    }

    @ShellMethod(key = "shortcut-set", value = "Cria atalho de comando via config")
    public String shortcutSet(
            @ShellOption(help = "Nome do atalho") String name,
            @ShellOption(help = "Comando alvo (use aspas)") String command) {
        configService.setShortcut(name, command);
        return styled(GREEN, t("Atalho salvo.", "Shortcut saved."));
    }

    @ShellMethod(key = "shortcut-rm", value = "Remove atalho")
    public String shortcutRm(@ShellOption(help = "Nome do atalho", valueProvider = ShortcutNameValueProvider.class) String name) {
        if (configService.removeShortcut(name)) return styled(GREEN, t("Atalho removido.", "Shortcut removed."));
        return styled(RED, t("Atalho nao encontrado.", "Shortcut not found."));
    }

    @ShellMethod(key = "shortcuts", value = "Lista atalhos")
    public String shortcuts() {
        var all = configService.listShortcuts();
        if (all.isEmpty()) return styled(DIM, t("Nenhum atalho configurado.", "No shortcuts configured."));
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  " + t("Atalhos", "Shortcuts") + "\n\n"));
        all.forEach((k, v) -> sb.append(styled(CYAN, "  " + k)).append(styled(DIM, " -> ")).append(v).append("\n"));
        return sb.toString();
    }

    @ShellMethod(key = {"shortcut-run", "s"}, value = "Executa atalho configurado")
    public String shortcutRun(
            @ShellOption(help = "Nome do atalho", valueProvider = ShortcutNameValueProvider.class) String name,
            @ShellOption(value = "--args", defaultValue = ShellOption.NULL, help = "Args extras") String args) {
        var cmd = configService.getShortcut(name).orElse(null);
        if (cmd == null) return styled(RED, t("Atalho nao encontrado.", "Shortcut not found."));
        var full = args == null || args.isBlank() ? cmd : cmd + " " + args;
        try {
            Object result = evaluate(full);
            return result != null ? result.toString() : styled(GREEN, t("Atalho executado.", "Shortcut executed."));
        } catch (Exception e) {
            return styled(RED, t("Erro ao executar atalho: ", "Error running shortcut: ") + e.getMessage());
        }
    }

    private Object evaluate(String raw) throws Exception {
        var shell = shellProvider.getObject();
        var method = Shell.class.getDeclaredMethod("evaluate", Input.class);
        method.setAccessible(true);
        return method.invoke(shell, (Input) () -> raw);
    }
}
