package com.hidariapi.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hidariapi.model.AppConfig;
import com.hidariapi.util.AppPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class ConfigStore {

    private static final Logger log = LoggerFactory.getLogger(ConfigStore.class);

    private static final Path CONFIG_DIR = AppPaths.configDir();
    private static final Path FILE = CONFIG_DIR.resolve("config.json");

    private final ObjectMapper mapper;

    public ConfigStore() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public AppConfig load() {
        if (!Files.exists(FILE)) return AppConfig.defaults();
        try {
            var cfg = mapper.readValue(FILE.toFile(), AppConfig.class);
            return normalize(cfg);
        } catch (IOException e) {
            log.warn("Could not load config: {}", e.getMessage());
            return AppConfig.defaults();
        }
    }

    public void save(AppConfig config) {
        try {
            Files.createDirectories(CONFIG_DIR);
            mapper.writeValue(FILE.toFile(), normalize(config));
        } catch (IOException e) {
            log.warn("Could not save config: {}", e.getMessage());
        }
    }

    private AppConfig normalize(AppConfig cfg) {
        if (cfg == null) return AppConfig.defaults();
        var normalized = cfg;
        if (normalized.language() == null) normalized = normalized.withLanguage(com.hidariapi.model.Language.PT);
        if (normalized.defaultCall() <= 0) normalized = normalized.withDefaultCall(1);
        if (normalized.defaultParallel() <= 0) normalized = normalized.withDefaultParallel(1);
        if (normalized.activeProfile() == null || normalized.activeProfile().isBlank()) normalized = normalized.withActiveProfile("default");
        return new AppConfig(
                normalized.language(),
                normalized.defaultCall(),
                normalized.defaultParallel(),
                normalized.defaultOutput(),
                normalized.activeProfile(),
                normalized.ensureProfiles()
        );
    }
}
