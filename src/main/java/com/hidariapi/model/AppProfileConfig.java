package com.hidariapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Per-profile settings.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppProfileConfig(
        String baseUrl
) {
    public static AppProfileConfig empty() {
        return new AppProfileConfig(null);
    }

    public AppProfileConfig withBaseUrl(String baseUrl) {
        return new AppProfileConfig(baseUrl);
    }
}
