package com.hidariapi.util;

import java.nio.file.Path;

/**
 * Centralizes OS-aware paths used by the application.
 */
public final class AppPaths {

    private static final String APP_NAME = "hidariapi";

    private AppPaths() {}

    public static Path configDir() {
        var explicit = env("HIDARIAPI_CONFIG_DIR");
        if (explicit != null) return Path.of(explicit);

        if (isWindows()) {
            var appData = env("APPDATA");
            if (appData != null) return Path.of(appData, APP_NAME);
            return Path.of(userHome(), "AppData", "Roaming", APP_NAME);
        }

        var xdgConfig = env("XDG_CONFIG_HOME");
        if (xdgConfig != null) return Path.of(xdgConfig, APP_NAME);
        return Path.of(userHome(), ".config", APP_NAME);
    }

    private static boolean isWindows() {
        var os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    private static String userHome() {
        return System.getProperty("user.home");
    }

    private static String env(String key) {
        var value = System.getenv(key);
        return value == null || value.isBlank() ? null : value;
    }
}
