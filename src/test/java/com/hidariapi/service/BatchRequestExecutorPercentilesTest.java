package com.hidariapi.service;

import com.hidariapi.model.ApiRequest;
import com.hidariapi.model.ApiResponse;
import com.hidariapi.model.HttpMethod;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BatchRequestExecutorPercentilesTest {

    @Test
    void computesPercentilesMinMaxAndAverage() {
        var req = ApiRequest.of("r", HttpMethod.GET, "https://api.test");
        var c1 = BatchRequestExecutor.CallResult.success(1, Instant.now(), req,
                new ApiResponse(200, Map.of(), "", Duration.ofMillis(10), "", 0));
        var c2 = BatchRequestExecutor.CallResult.success(2, Instant.now(), req,
                new ApiResponse(200, Map.of(), "", Duration.ofMillis(20), "", 0));
        var c3 = BatchRequestExecutor.CallResult.success(3, Instant.now(), req,
                new ApiResponse(200, Map.of(), "", Duration.ofMillis(30), "", 0));
        var c4 = BatchRequestExecutor.CallResult.failure(4, Instant.now(), req, "boom");

        var result = new BatchRequestExecutor.BatchExecutionResult(
                Instant.now(), Instant.now(), 4, 3, 1, 60, List.of(c1, c2, c3, c4)
        );

        assertEquals(15, result.averageDurationMs());
        assertEquals(10, result.minDurationMs());
        assertEquals(30, result.maxDurationMs());
        assertEquals(20, result.p50DurationMs());
        assertEquals(30, result.p95DurationMs());
        assertEquals(30, result.p99DurationMs());
    }
}
