package com.hidariapi.service;

import com.hidariapi.model.AppConfig;
import com.hidariapi.model.AppProfileConfig;
import com.hidariapi.model.Language;
import com.hidariapi.store.ConfigStore;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ConfigService {

    private final ConfigStore store;
    private AppConfig config;

    public ConfigService(ConfigStore store) {
        this.store = store;
        this.config = store.load();
    }

    public AppConfig get() {
        return config;
    }

    public void setLanguage(Language language) {
        config = config.withLanguage(language);
        persist();
    }

    public Language getLanguage() {
        return config.language();
    }

    public int getDefaultCall() {
        return config.defaultCall();
    }

    public int getDefaultParallel() {
        return config.defaultParallel();
    }

    public String getDefaultOutput() {
        return config.defaultOutput();
    }

    public void setDefaultCall(int value) {
        config = config.withDefaultCall(Math.max(1, value));
        persist();
    }

    public void setDefaultParallel(int value) {
        config = config.withDefaultParallel(Math.max(1, value));
        persist();
    }

    public void setDefaultOutput(String value) {
        config = config.withDefaultOutput(value != null && !value.isBlank() ? value : null);
        persist();
    }

    public String getActiveProfile() {
        return config.activeProfile();
    }

    public void useProfile(String profile) {
        String p = normalize(profile);
        config = config.withActiveProfile(p);
        persist();
    }

    public Map<String, AppProfileConfig> listProfiles() {
        return config.ensureProfiles();
    }

    public void setProfileBaseUrl(String profile, String baseUrl) {
        String p = normalize(profile);
        config = config.withProfileBaseUrl(p, baseUrl);
        persist();
    }

    public String getActiveBaseUrl() {
        var profiles = config.ensureProfiles();
        var active = profiles.getOrDefault(config.activeProfile(), AppProfileConfig.empty());
        return active.baseUrl();
    }

    public String resolveUrlWithBaseProfile(String url) {
        if (url == null || url.isBlank()) return url;
        if (url.contains("://") || url.startsWith("{{")) return url;
        var base = getActiveBaseUrl();
        if (base == null || base.isBlank()) return url;
        if (base.endsWith("/") && url.startsWith("/")) return base.substring(0, base.length() - 1) + url;
        if (!base.endsWith("/") && !url.startsWith("/")) return base + "/" + url;
        return base + url;
    }

    public Map<String, String> listFlat() {
        var out = new LinkedHashMap<String, String>();
        out.put("language", config.language().name().toLowerCase());
        out.put("request.default-call", String.valueOf(config.defaultCall()));
        out.put("request.default-parallel", String.valueOf(config.defaultParallel()));
        out.put("request.default-output", config.defaultOutput() != null ? config.defaultOutput() : "");
        out.put("profile.active", config.activeProfile());
        for (var e : config.ensureProfiles().entrySet()) {
            out.put("profile." + e.getKey() + ".base-url", e.getValue().baseUrl() != null ? e.getValue().baseUrl() : "");
        }
        return out;
    }

    public String getByKey(String key) {
        return listFlat().get(key);
    }

    public boolean setByKey(String key, String value) {
        if (key == null || key.isBlank()) return false;
        switch (key) {
            case "language" -> setLanguage(Language.fromString(value));
            case "request.default-call" -> setDefaultCall(parseInt(value, 1));
            case "request.default-parallel" -> setDefaultParallel(parseInt(value, 1));
            case "request.default-output" -> setDefaultOutput(value);
            case "profile.active" -> useProfile(value);
            default -> {
                if (key.startsWith("profile.") && key.endsWith(".base-url")) {
                    var profile = key.substring("profile.".length(), key.length() - ".base-url".length());
                    setProfileBaseUrl(profile, value);
                    return true;
                }
                return false;
            }
        }
        return true;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase();
    }

    private void persist() {
        store.save(config);
    }
}
