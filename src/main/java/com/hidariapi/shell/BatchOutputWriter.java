package com.hidariapi.shell;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hidariapi.model.ApiRequest;
import com.hidariapi.service.BatchRequestExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes batch execution outputs to JSON files.
 */
@Component
public class BatchOutputWriter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void write(String filePath, BatchRequestExecutor.BatchExecutionResult result) throws IOException {
        var request = result.calls().isEmpty() ? null : result.calls().getFirst().request();
        var payload = new LinkedHashMap<String, Object>();
        payload.put("startedAt", result.startedAt().toString());
        payload.put("finishedAt", result.finishedAt().toString());
        payload.put("calls", result.callCount());
        payload.put("success", result.success());
        payload.put("failed", result.failed());
        payload.put("totalDurationMs", result.totalDurationMs());
        payload.put("averageDurationMs", result.averageDurationMs());
        payload.put("requestTemplate", request != null ? requestMap(request) : null);
        payload.put("responses", result.calls().stream().map(this::responseMap).toList());

        var path = normalizeOutputPath(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), payload);
    }

    private Map<String, Object> responseMap(BatchRequestExecutor.CallResult call) {
        var entry = new LinkedHashMap<String, Object>();
        entry.put("index", call.index());
        entry.put("timestamp", call.timestamp().toString());
        entry.put("request", requestMap(call.request()));

        if (call.response() != null) {
            var response = call.response();
            entry.put("success", call.isSuccess());
            entry.put("statusCode", response.statusCode());
            entry.put("statusText", response.statusText());
            entry.put("durationMs", response.duration().toMillis());
            entry.put("size", response.size());
            entry.put("contentType", response.contentType());
            entry.put("headers", response.headers());
            entry.put("body", response.body());
            entry.put("error", null);
        } else {
            entry.put("success", false);
            entry.put("statusCode", null);
            entry.put("statusText", null);
            entry.put("durationMs", null);
            entry.put("size", null);
            entry.put("contentType", null);
            entry.put("headers", Map.of());
            entry.put("body", null);
            entry.put("error", call.error());
        }
        return entry;
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
