package com.hidariapi.service;

import com.hidariapi.model.ApiRequest;
import com.hidariapi.model.ApiResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
        return execute(request, callCount, 1);
    }

    public BatchExecutionResult execute(ApiRequest request, int callCount, int parallelism) {
        if (parallelism <= 1 || callCount <= 1) {
            return executeSequential(request, callCount);
        }
        return executeParallel(request, callCount, parallelism);
    }

    private BatchExecutionResult executeSequential(ApiRequest request, int callCount) {
        var startedAt = Instant.now();
        int success = 0;
        int failed = 0;
        long totalMs = 0;
        var calls = new ArrayList<CallResult>();

        for (int i = 1; i <= callCount; i++) {
            try {
                var response = apiService.execute(request);
                boolean ok = response.statusCode() < 400;
                if (ok)
                    success++;
                else
                    failed++;
                totalMs += response.duration().toMillis();
                calls.add(CallResult.success(i, Instant.now(), request, response));
            } catch (Exception e) {
                failed++;
                calls.add(CallResult.failure(i, Instant.now(), request, e.getMessage()));
            }
        }

        return new BatchExecutionResult(startedAt, Instant.now(), callCount, success, failed, totalMs, calls);
    }

    private BatchExecutionResult executeParallel(ApiRequest request, int callCount, int parallelism) {
        var startedAt = Instant.now();
        var executor = Executors.newFixedThreadPool(Math.min(parallelism, callCount));
        var futures = new ArrayList<Future<CallResult>>(callCount);
        try {
            for (int i = 1; i <= callCount; i++) {
                final int index = i;
                Callable<CallResult> task = () -> executeCall(index, request);
                futures.add(executor.submit(task));
            }

            var calls = new ArrayList<CallResult>(callCount);
            int success = 0;
            int failed = 0;
            long totalMs = 0;
            for (var future : futures) {
                try {
                    var call = future.get();
                    calls.add(call);
                    if (call.response() != null && call.response().statusCode() < 400) {
                        success++;
                        totalMs += call.response().duration().toMillis();
                    } else if (call.response() != null) {
                        failed++;
                        totalMs += call.response().duration().toMillis();
                    } else {
                        failed++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failed++;
                    calls.add(CallResult.failure(calls.size() + 1, Instant.now(), request, e.getMessage()));
                } catch (ExecutionException e) {
                    failed++;
                    var message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    calls.add(CallResult.failure(calls.size() + 1, Instant.now(), request, message));
                }
            }

            calls.sort(Comparator.comparingInt(CallResult::index));
            return new BatchExecutionResult(startedAt, Instant.now(), callCount, success, failed, totalMs, calls);
        } finally {
            executor.shutdownNow();
        }
    }

    private CallResult executeCall(int index, ApiRequest request) {
        try {
            var response = apiService.execute(request);
            return CallResult.success(index, Instant.now(), request, response);
        } catch (Exception e) {
            return CallResult.failure(index, Instant.now(), request, e.getMessage());
        }
    }

    public record BatchExecutionResult(
            Instant startedAt,
            Instant finishedAt,
            int callCount,
            int success,
            int failed,
            long totalDurationMs,
            List<CallResult> calls) {
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
            if (values.isEmpty())
                return 0;
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
            String error) {
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
