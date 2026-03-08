package com.hidariapi.shell;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hidariapi.model.ApiRequest;
import com.hidariapi.model.ApiResponse;
import com.hidariapi.model.HttpMethod;
import com.hidariapi.service.BatchRequestExecutor;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BatchOutputWriterPercentilesTest {

    @Test
    void writesPercentileFieldsInOutputJson() throws Exception {
        var writer = new BatchOutputWriter();
        var req = ApiRequest.of("r", HttpMethod.GET, "https://api.test");
        var c1 = BatchRequestExecutor.CallResult.success(1, Instant.now(), req,
                new ApiResponse(200, Map.of(), "", Duration.ofMillis(10), "", 0));
        var c2 = BatchRequestExecutor.CallResult.success(2, Instant.now(), req,
                new ApiResponse(200, Map.of(), "", Duration.ofMillis(100), "", 0));

        var result = new BatchRequestExecutor.BatchExecutionResult(
                Instant.now(), Instant.now(), 2, 2, 0, 110, List.of(c1, c2)
        );

        Path out = Path.of("target/test-home/out/batch-percentiles.json");
        writer.write(out.toString(), result);

        var tree = new ObjectMapper().readTree(Files.readString(out));
        assertEquals(10, tree.get("minDurationMs").asInt());
        assertEquals(100, tree.get("maxDurationMs").asInt());
        assertEquals(10, tree.get("p50DurationMs").asInt());
        assertEquals(100, tree.get("p95DurationMs").asInt());
        assertEquals(100, tree.get("p99DurationMs").asInt());
    }
}
