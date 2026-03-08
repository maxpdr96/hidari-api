package com.hidariapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized user configuration.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppConfig(
        Language language,
        int defaultCall,
        int defaultParallel,
        String defaultOutput,
        String activeProfile,
        Map<String, AppProfileConfig> profiles,
        Map<String, String> shortcuts
) {
    public static AppConfig defaults() {
        var profiles = new LinkedHashMap<String, AppProfileConfig>();
        profiles.put("default", AppProfileConfig.empty());
        return new AppConfig(Language.PT, 1, 1, null, "default", profiles, new LinkedHashMap<>());
    }

    public AppConfig withLanguage(Language language) {
        return new AppConfig(language, defaultCall, defaultParallel, defaultOutput, activeProfile, profiles, shortcuts);
    }

    public AppConfig withDefaultCall(int value) {
        return new AppConfig(language, value, defaultParallel, defaultOutput, activeProfile, profiles, shortcuts);
    }

    public AppConfig withDefaultParallel(int value) {
        return new AppConfig(language, defaultCall, value, defaultOutput, activeProfile, profiles, shortcuts);
    }

    public AppConfig withDefaultOutput(String value) {
        return new AppConfig(language, defaultCall, defaultParallel, value, activeProfile, profiles, shortcuts);
    }

    public AppConfig withActiveProfile(String profile) {
        var p = ensureProfiles();
        p.putIfAbsent(profile, AppProfileConfig.empty());
        return new AppConfig(language, defaultCall, defaultParallel, defaultOutput, profile, p, ensureShortcuts());
    }

    public AppConfig withProfileBaseUrl(String profile, String baseUrl) {
        var p = ensureProfiles();
        var existing = p.getOrDefault(profile, AppProfileConfig.empty());
        p.put(profile, existing.withBaseUrl(baseUrl));
        return new AppConfig(language, defaultCall, defaultParallel, defaultOutput, activeProfile, p, ensureShortcuts());
    }

    public AppConfig withShortcut(String name, String command) {
        var s = ensureShortcuts();
        s.put(name, command);
        return new AppConfig(language, defaultCall, defaultParallel, defaultOutput, activeProfile, ensureProfiles(), s);
    }

    public AppConfig withoutShortcut(String name) {
        var s = ensureShortcuts();
        s.remove(name);
        return new AppConfig(language, defaultCall, defaultParallel, defaultOutput, activeProfile, ensureProfiles(), s);
    }

    public Map<String, AppProfileConfig> ensureProfiles() {
        var p = profiles != null ? new LinkedHashMap<>(profiles) : new LinkedHashMap<String, AppProfileConfig>();
        p.putIfAbsent("default", AppProfileConfig.empty());
        return p;
    }

    public Map<String, String> ensureShortcuts() {
        return shortcuts != null ? new LinkedHashMap<>(shortcuts) : new LinkedHashMap<>();
    }
}
