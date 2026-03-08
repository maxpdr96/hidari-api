package com.hidariapi.shell;

import com.hidariapi.service.ConfigService;
import com.hidariapi.service.LanguageService;
import org.springframework.stereotype.Component;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

@Component
public class ConfigCommands extends LocalizedSupport {

    private final ConfigService configService;

    public ConfigCommands(ConfigService configService, LanguageService lang) {
        super(lang);
        this.configService = configService;
    }

    @Command(name = "config-set", description = "Define valor de configuracao")
    public String configSet(
            @Option(description = "Chave (ex: language, request.default-parallel, profile.dev.base-url)", required = true) String key,
            @Option(description = "Valor", required = true) String value) {
        boolean ok = configService.setByKey(key, value);
        if (!ok) return styled(RED, t("Chave invalida: ", "Invalid key: ") + key);
        return styled(GREEN, t("Configuracao atualizada.", "Configuration updated."));
    }

    @Command(name = "config-get", description = "Le valor de configuracao")
    public String configGet(@Option(description = "Chave", required = true) String key) {
        var value = configService.getByKey(key);
        if (value == null) return styled(RED, t("Chave nao encontrada: ", "Key not found: ") + key);
        return styled(CYAN, key) + styled(DIM, " = ") + styled(WHITE, value);
    }

    @Command(name = "config-list", description = "Lista configuracoes")
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

    @Command(name = "profile-use", description = "Ativa profile de configuracao")
    public String profileUse(@Option(description = "Nome do profile", required = true) String name) {
        configService.useProfile(name);
        return styled(GREEN, t("Profile ativo: ", "Active profile: ") + configService.getActiveProfile());
    }

    @Command(name = "profile-set-base-url", description = "Define base_url de um profile")
    public String profileSetBaseUrl(
            @Option(description = "Nome do profile", required = true) String profile,
            @Option(description = "Base URL", required = true) String baseUrl) {
        configService.setProfileBaseUrl(profile, baseUrl);
        return styled(GREEN, t("Base URL definida para ", "Base URL set for ") + profile + ": " + baseUrl);
    }

    @Command(name = "profile-list", description = "Lista profiles de configuracao")
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
