package com.hidariapi.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void statusDurationAndSizeFormattingWorks() {
        var res = new ApiResponse(200, Map.of(), "{}", Duration.ofMillis(1500), "application/json", 1536);

        assertEquals("200 OK", res.statusText());
        assertEquals("1.5s", res.durationText());
        assertEquals("1.5 KB", res.sizeText());
    }

    @Test
    void jsonAndSuccessFlagsAreComputed() {
        var ok = new ApiResponse(201, Map.of(), "{}", Duration.ofMillis(10), "application/json", 2);
        var err = new ApiResponse(404, Map.of(), "", Duration.ofMillis(10), "text/plain", 0);

        assertTrue(ok.isJson());
        assertTrue(ok.isSuccess());
        assertFalse(err.isJson());
        assertFalse(err.isSuccess());
    }
}
