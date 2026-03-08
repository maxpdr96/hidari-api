package com.hidariapi.service;

import com.hidariapi.model.Language;
import com.hidariapi.util.AppPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manages the current display language.
 */
@Service
public class LanguageService {

    private static final Logger log = LoggerFactory.getLogger(LanguageService.class);

    private static final Path CONFIG_DIR = AppPaths.configDir();
    private static final Path FILE = CONFIG_DIR.resolve("language");

    private Language current;

    public LanguageService() {
        this.current = load();
    }

    /** Returns the current language. */
    public Language getCurrent() {
        return current;
    }

    /** Sets and persists the language. */
    public void setCurrent(Language language) {
        this.current = language;
        persist();
    }

    /** Returns true if the current language is English. */
    public boolean isEnglish() {
        return current == Language.EN;
    }

    /** Returns the PT or EN text based on current language. */
    public String t(String pt, String en) {
        return current == Language.EN ? en : pt;
    }

    private Language load() {
        if (!Files.exists(FILE)) return Language.PT;
        try {
            var content = Files.readString(FILE).trim();
            return Language.fromString(content);
        } catch (IOException e) {
            log.warn("Could not load language setting: {}", e.getMessage());
            return Language.PT;
        }
    }

    private void persist() {
        try {
            Files.createDirectories(CONFIG_DIR);
            Files.writeString(FILE, current.name());
        } catch (IOException e) {
            log.warn("Could not save language setting: {}", e.getMessage());
        }
    }
}
