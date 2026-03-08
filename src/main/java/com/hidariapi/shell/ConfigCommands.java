package com.hidariapi.shell;

import com.hidariapi.service.ConfigService;
import com.hidariapi.service.LanguageService;
import com.hidariapi.shell.completion.ProfileNameValueProvider;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class ConfigCommands extends LocalizedSupport {

    private final ConfigService configService;

    public ConfigCommands(ConfigService configService, LanguageService lang) {
        super(lang);
        this.configService = configService;
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
}
