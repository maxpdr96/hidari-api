package com.hidariapi.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppPathsTest {

    private final String originalOs = System.getProperty("os.name");
    private final String originalHome = System.getProperty("user.home");

    @AfterEach
    void restoreProperties() {
        System.setProperty("os.name", originalOs);
        System.setProperty("user.home", originalHome);
    }

    @Test
    void returnsWindowsFallbackWhenOsIsWindows() {
        System.setProperty("os.name", "Windows 11");
        System.setProperty("user.home", "/tmp/home");

        Path path = AppPaths.configDir();
        assertTrue(path.toString().contains("hidariapi"));
    }

    @Test
    void returnsLinuxFallbackWhenOsIsLinux() {
        System.setProperty("os.name", "Linux");
        System.setProperty("user.home", "/tmp/home");

        Path path = AppPaths.configDir();
        assertTrue(path.endsWith(Path.of(".config", "hidariapi")));
    }
}
