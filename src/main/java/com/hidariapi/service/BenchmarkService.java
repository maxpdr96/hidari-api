package com.hidariapi.service;

import com.hidariapi.model.ApiRequest;
import com.hidariapi.model.ApiResponse;
import com.hidariapi.util.UrlSanitizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Dedicated benchmark runner for high-volume request measurements.
 */
@Service
public class BenchmarkService {

    private final HttpClient httpClient;
    private final int timeoutSeconds;

    public BenchmarkService(@Value("${hidariapi.timeout-seconds:30}") int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    public BenchResult run(ApiRequest request, int calls, int concurrency, int warmup) {
        if (calls <= 0 || concurrency <= 0 || warmup < 0) {
            throw new IllegalArgumentException("Invalid benchmark parameters.");
        }

        for (int i = 0; i < warmup; i++) {
            try {
                executeOnce(request);
            } catch (Exception ignored) {
                // Warmup failures are intentionally ignored.
            }
        }

        var executor = Executors.newFixedThreadPool(concurrency);
        var tasks = new ArrayList<Callable<CallMetric>>();
        for (int i = 1; i <= calls; i++) {
            final int index = i;
            tasks.add(() -> {
                try {
                    var response = executeOnce(request);
                    return CallMetric.success(index, response.duration().toMillis(), response.statusCode());
                } catch (Exception e) {
                    return CallMetric.failure(index, e.getMessage());
                }
            });
        }

        var startedNs = System.nanoTime();
        var futures = new ArrayList<Future<CallMetric>>();
        for (var task : tasks) futures.add(executor.submit(task));

        var metrics = new ArrayList<CallMetric>();
        for (var future : futures) {
            try {
                metrics.add(future.get());
            } catch (Exception e) {
                metrics.add(CallMetric.failure(-1, e.getMessage()));
            }
        }
        executor.shutdown();
        long elapsedMs = Duration.ofNanos(System.nanoTime() - startedNs).toMillis();

        int success = 0;
        int failed = 0;
        long totalLatencyMs = 0;
        var durations = new ArrayList<Long>();
        for (var m : metrics) {
            if (m.error() == null) {
                success++;
                durations.add(m.durationMs());
                totalLatencyMs += m.durationMs();
            } else {
                failed++;
            }
        }

        return new BenchResult(
                request.method().name(),
                request.url(),
                calls,
                concurrency,
                warmup,
                success,
                failed,
                elapsedMs,
                totalLatencyMs,
                durations,
                metrics
        );
    }

    private ApiResponse executeOnce(ApiRequest request) throws Exception {
        var builder = HttpRequest.newBuilder()
                .uri(UrlSanitizer.toUri(request.url()))
                .timeout(Duration.ofSeconds(timeoutSeconds));

        for (var h : request.headers().entrySet()) {
            builder.header(h.getKey(), h.getValue());
        }
        var bodyPublisher = request.body() != null && !request.body().isBlank()
                ? HttpRequest.BodyPublishers.ofString(request.body())
                : HttpRequest.BodyPublishers.noBody();
        builder.method(request.method().name(), bodyPublisher);

        long start = System.nanoTime();
        var response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        long ms = Duration.ofNanos(System.nanoTime() - start).toMillis();

        var headers = new LinkedHashMap<String, String>();
        response.headers().map().forEach((k, v) -> headers.put(k, String.join(", ", v)));
        var contentType = headers.getOrDefault("content-type", "");
        var body = response.body() != null ? response.body() : "";

        return new ApiResponse(
                response.statusCode(),
                headers,
                body,
                Duration.ofMillis(ms),
                contentType,
                body.length()
        );
    }

    public record BenchResult(
            String method,
            String url,
            int calls,
            int concurrency,
            int warmup,
            int success,
            int failed,
            long wallTimeMs,
            long totalLatencyMs,
            List<Long> latencies,
            List<CallMetric> metrics
    ) {
        public long averageMs() {
            return success > 0 ? totalLatencyMs / success : 0;
        }

        public long minMs() {
            return latencies.stream().min(Comparator.naturalOrder()).orElse(0L);
        }

        public long maxMs() {
            return latencies.stream().max(Comparator.naturalOrder()).orElse(0L);
        }

        public long percentile(int p) {
            if (latencies.isEmpty()) return 0;
            var sorted = new ArrayList<>(latencies);
            sorted.sort(Long::compareTo);
            int index = (int) Math.ceil((p / 100.0) * sorted.size()) - 1;
            index = Math.max(0, Math.min(index, sorted.size() - 1));
            return sorted.get(index);
        }

        public long p50() { return percentile(50); }

        public long p95() { return percentile(95); }

        public long p99() { return percentile(99); }

        public double rps() {
            if (wallTimeMs <= 0) return 0.0;
            return calls / (wallTimeMs / 1000.0);
        }
    }

    public record CallMetric(int index, long durationMs, Integer statusCode, String error) {
        public static CallMetric success(int index, long durationMs, int statusCode) {
            return new CallMetric(index, durationMs, statusCode, null);
        }

        public static CallMetric failure(int index, String error) {
            return new CallMetric(index, 0, null, error);
        }
    }
}
