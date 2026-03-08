package com.hidariapi.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SavedRequestTest {

    @Test
    void fromBuildsEntryWithResponseData() {
        var req = ApiRequest.of("r", HttpMethod.GET, "https://a");
        var res = new ApiResponse(200, Map.of(), "{}", Duration.ofMillis(250), "application/json", 2);

        var saved = SavedRequest.from(req, res);
        assertEquals(200, saved.statusCode());
        assertEquals(250, saved.duration());
        assertEquals(req, saved.request());
        assertNotNull(saved.timestamp());
    }

    @Test
    void errorBuildsEntryWithZeroStatusAndDuration() {
        var req = ApiRequest.of("r", HttpMethod.GET, "https://a");
        var saved = SavedRequest.error(req);

        assertEquals(0, saved.statusCode());
        assertEquals(0, saved.duration());
        assertEquals(req, saved.request());
    }
}
