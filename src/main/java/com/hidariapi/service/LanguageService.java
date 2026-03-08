package com.hidariapi.service;

import com.hidariapi.model.Language;
import org.springframework.stereotype.Service;

/**
 * Manages the current display language.
 */
@Service
public class LanguageService {

    private final ConfigService configService;

    public LanguageService(ConfigService configService) {
        this.configService = configService;
    }

    /** Returns the current language. */
    public Language getCurrent() {
        return configService.getLanguage();
    }

    /** Sets and persists the language. */
    public void setCurrent(Language language) {
        configService.setLanguage(language);
    }

    /** Returns true if the current language is English. */
    public boolean isEnglish() {
        return getCurrent() == Language.EN;
    }

    /** Returns the PT or EN text based on current language. */
    public String t(String pt, String en) {
        return getCurrent() == Language.EN ? en : pt;
    }
}
