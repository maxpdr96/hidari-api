package com.hidariapi.shell;

import com.hidariapi.model.Language;
import com.hidariapi.service.ConfigService;
import com.hidariapi.service.LanguageService;
import com.hidariapi.store.ConfigStore;
import com.hidariapi.util.AppPaths;
import com.hidariapi.util.TestFs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalizedHelpRendererTest {

    @BeforeEach
    void clean() throws Exception {
        TestFs.deleteRecursively(AppPaths.configDir());
    }

    @Test
    void rendersInPortugueseAndEnglish() {
        var lang = new LanguageService(new ConfigService(new ConfigStore()));
        var renderer = new LocalizedHelpRenderer(lang);

        lang.setCurrent(Language.PT);
        var pt = renderer.render();
        assertTrue(pt.contains("Ajuda"));
        assertTrue(pt.contains("Requisicoes HTTP"));

        lang.setCurrent(Language.EN);
        var en = renderer.render();
        assertTrue(en.contains("Help"));
        assertTrue(en.contains("HTTP Requests"));
    }
}
