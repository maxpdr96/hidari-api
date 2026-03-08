package com.hidariapi.service;

import com.hidariapi.model.Language;
import com.hidariapi.util.AppPaths;
import com.hidariapi.util.TestFs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LanguageServiceTest {

    @BeforeEach
    void clean() throws Exception {
        TestFs.deleteRecursively(AppPaths.configDir());
    }

    @Test
    void defaultsToPtAndPersistsSelection() {
        var service = new LanguageService();
        assertEquals(Language.PT, service.getCurrent());

        service.setCurrent(Language.EN);
        assertTrue(service.isEnglish());
        assertEquals("en", service.t("pt", "en"));

        var reloaded = new LanguageService();
        assertEquals(Language.EN, reloaded.getCurrent());
    }
}
