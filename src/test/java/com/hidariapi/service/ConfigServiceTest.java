package com.hidariapi.service;

import com.hidariapi.model.Language;
import com.hidariapi.store.ConfigStore;
import com.hidariapi.util.AppPaths;
import com.hidariapi.util.TestFs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigServiceTest {

    @BeforeEach
    void clean() throws Exception {
        TestFs.deleteRecursively(AppPaths.configDir());
    }

    @Test
    void persistsLanguageDefaultsAndProfiles() {
        var service = new ConfigService(new ConfigStore());
        assertEquals(Language.PT, service.getLanguage());
        assertEquals(1, service.getDefaultCall());
        assertEquals("default", service.getActiveProfile());

        service.setLanguage(Language.EN);
        service.setDefaultCall(5);
        service.setDefaultParallel(3);
        service.setDefaultOutput("out.json");
        service.setProfileBaseUrl("dev", "http://localhost:8080");
        service.useProfile("dev");

        var reloaded = new ConfigService(new ConfigStore());
        assertEquals(Language.EN, reloaded.getLanguage());
        assertEquals(5, reloaded.getDefaultCall());
        assertEquals(3, reloaded.getDefaultParallel());
        assertEquals("out.json", reloaded.getDefaultOutput());
        assertEquals("dev", reloaded.getActiveProfile());
        assertEquals("http://localhost:8080/health", reloaded.resolveUrlWithBaseProfile("/health"));
    }

    @Test
    void setByKeyWorksForRequestedKeys() {
        var service = new ConfigService(new ConfigStore());

        assertTrue(service.setByKey("language", "en"));
        assertTrue(service.setByKey("request.default-call", "10"));
        assertTrue(service.setByKey("request.default-parallel", "4"));
        assertTrue(service.setByKey("profile.active", "staging"));
        assertTrue(service.setByKey("profile.staging.base-url", "https://api.staging"));
        assertFalse(service.setByKey("invalid.key", "x"));

        assertEquals("en", service.getByKey("language"));
        assertEquals("10", service.getByKey("request.default-call"));
        assertEquals("4", service.getByKey("request.default-parallel"));
        assertEquals("staging", service.getByKey("profile.active"));
        assertEquals("https://api.staging", service.getByKey("profile.staging.base-url"));
    }
}
