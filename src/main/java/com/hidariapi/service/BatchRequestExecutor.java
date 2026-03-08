package com.hidariapi.service;

import com.hidariapi.model.ApiRequest;
import com.hidariapi.model.ApiResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Executes the same request multiple times and aggregates results.
 */
@Service
public class BatchRequestExecutor {

    private final ApiService apiService;

    public BatchRequestExecutor(ApiService apiService) {
        this.apiService = apiService;
    }

    public BatchExecutionResult execute(ApiRequest request, int callCount) {
        var startedAt = Instant.now();
        int success = 0;
        int failed = 0;
        long totalMs = 0;
        var calls = new ArrayList<CallResult>();

        for (int i = 1; i <= callCount; i++) {
            try {
                var response = apiService.execute(request);
                var executedRequest = apiService.getLastRequest() != null ? apiService.getLastRequest() : request;
                boolean ok = response.statusCode() < 400;
                if (ok) success++;
                else failed++;
                totalMs += response.duration().toMillis();
                calls.add(CallResult.success(i, Instant.now(), executedRequest, response));
            } catch (Exception e) {
                failed++;
                calls.add(CallResult.failure(i, Instant.now(), request, e.getMessage()));
            }
        }

        return new BatchExecutionResult(startedAt, Instant.now(), callCount, success, failed, totalMs, calls);
    }

    public record BatchExecutionResult(
            Instant startedAt,
            Instant finishedAt,
            int callCount,
            int success,
            int failed,
            long totalDurationMs,
            List<CallResult> calls
    ) {
        public long averageDurationMs() {
            return callCount > 0 ? totalDurationMs / callCount : 0;
        }

        public long minDurationMs() {
            return successfulDurations().stream().min(Comparator.naturalOrder()).orElse(0L);
        }

        public long maxDurationMs() {
            return successfulDurations().stream().max(Comparator.naturalOrder()).orElse(0L);
        }

        public long p50DurationMs() {
            return percentile(50);
        }

        public long p95DurationMs() {
            return percentile(95);
        }

        public long p99DurationMs() {
            return percentile(99);
        }

        public long percentile(int p) {
            var values = successfulDurations();
            if (values.isEmpty()) return 0;
            values.sort(Long::compareTo);
            int index = (int) Math.ceil((p / 100.0) * values.size()) - 1;
            index = Math.max(0, Math.min(index, values.size() - 1));
            return values.get(index);
        }

        private List<Long> successfulDurations() {
            var values = new ArrayList<Long>();
            for (var call : calls) {
                if (call.response() != null) {
                    values.add(call.response().duration().toMillis());
                }
            }
            return values;
        }
    }

    public record CallResult(
            int index,
            Instant timestamp,
            ApiRequest request,
            ApiResponse response,
            String error
    ) {
        public static CallResult success(int index, Instant timestamp, ApiRequest request, ApiResponse response) {
            return new CallResult(index, timestamp, request, response, null);
        }

        public static CallResult failure(int index, Instant timestamp, ApiRequest request, String error) {
            return new CallResult(index, timestamp, request, null, error);
        }

        public boolean isSuccess() {
            return response != null && response.statusCode() < 400;
        }
    }
}
