package com.hidariapi.shell;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hidariapi.model.ApiRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes batch execution outputs to JSON files.
 */
@Component
public class BatchOutputWriter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void write(
            String filePath,
            ApiRequest request,
            int callCount,
            int success,
            int failed,
            long totalMs,
            String startedAt,
            List<Map<String, Object>> responses) throws IOException {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("startedAt", startedAt);
        payload.put("finishedAt", Instant.now().toString());
        payload.put("calls", callCount);
        payload.put("success", success);
        payload.put("failed", failed);
        payload.put("totalDurationMs", totalMs);
        payload.put("averageDurationMs", callCount > 0 ? totalMs / callCount : 0);
        payload.put("requestTemplate", requestMap(request));
        payload.put("responses", responses);

        var path = normalizeOutputPath(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), payload);
    }

    private Map<String, Object> requestMap(ApiRequest request) {
        var map = new LinkedHashMap<String, Object>();
        map.put("method", request.method().name());
        map.put("url", request.url());
        map.put("headers", request.headers());
        map.put("body", request.body());
        return map;
    }

    private Path normalizeOutputPath(String filePath) {
        var normalized = filePath != null && filePath.startsWith("@")
                ? filePath.substring(1)
                : filePath;
        return Path.of(normalized);
    }
}
