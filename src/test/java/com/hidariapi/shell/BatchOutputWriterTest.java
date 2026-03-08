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

import static org.junit.jupiter.api.Assertions.*;

class BatchOutputWriterTest {

    @Test
    void writesJsonFileUsingAtPrefixPath() throws Exception {
        var writer = new BatchOutputWriter();
        var req = ApiRequest.of("r", HttpMethod.GET, "https://api.test");
        var res = new ApiResponse(200, Map.of("content-type", "application/json"), "{}", Duration.ofMillis(10), "application/json", 2);

        var call = BatchRequestExecutor.CallResult.success(1, Instant.now(), req, res);
        var result = new BatchRequestExecutor.BatchExecutionResult(
                Instant.now(), Instant.now(), 1, 1, 0, 10, List.of(call)
        );

        Path out = Path.of("target/test-home/out/batch.json");
        writer.write("@" + out, result);

        assertTrue(Files.exists(out));
        var tree = new ObjectMapper().readTree(Files.readString(out));
        assertEquals(1, tree.get("calls").asInt());
        assertEquals(1, tree.get("responses").size());
        assertEquals(200, tree.get("responses").get(0).get("statusCode").asInt());
    }
}
