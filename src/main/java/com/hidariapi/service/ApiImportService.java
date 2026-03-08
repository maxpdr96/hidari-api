package com.hidariapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hidariapi.model.ApiRequest;
import com.hidariapi.model.Collection;
import com.hidariapi.model.HttpMethod;
import com.hidariapi.model.MockRoute;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class ApiImportService {

    private static final List<String> HTTP_METHODS = List.of("get", "post", "put", "patch", "delete", "head", "options");

    private final ApiService apiService;
    private final MockServerService mockServerService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ApiImportService(ApiService apiService, MockServerService mockServerService) {
        this.apiService = apiService;
        this.mockServerService = mockServerService;
    }

    public ImportResult importOpenApi(String json, String collectionName, String baseUrlOverride, boolean createMocks) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode paths = root.path("paths");
        if (!paths.isObject()) {
            throw new IllegalArgumentException("Invalid OpenAPI: missing 'paths'");
        }

        String defaultName = textOr(root.path("info").path("title"), "openapi-import");
        String collection = nonBlank(collectionName, defaultName);
        String baseUrl = nonBlank(baseUrlOverride, detectOpenApiBaseUrl(root));
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "{{base_url}}";

        var requests = new ArrayList<ApiRequest>();
        int mocks = 0;

        Iterator<String> pathNames = paths.fieldNames();
        while (pathNames.hasNext()) {
            String path = pathNames.next();
            JsonNode pathNode = paths.path(path);
            for (String methodName : HTTP_METHODS) {
                JsonNode op = pathNode.path(methodName);
                if (!op.isObject()) continue;

                HttpMethod method = HttpMethod.fromString(methodName);
                String opName = textOr(op.path("operationId"), method.name() + " " + path);
                String url = joinUrl(baseUrl, path);
                var req = ApiRequest.of(opName, method, url);

                if (hasJsonRequestBody(op)) {
                    req = req.withHeader("Content-Type", "application/json");
                    req = req.withBody(defaultJsonBody(op));
                }
                requests.add(req);

                if (createMocks) {
                    var mock = buildMockFromOpenApi(path, method, op);
                    mockServerService.addRoute(mock);
                    mocks++;
                }
            }
        }

        apiService.saveCollection(new Collection(collection, requests, java.time.Instant.now()));
        return new ImportResult(collection, requests.size(), mocks);
    }

    public ImportResult importPostman(String json, String collectionName) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode info = root.path("info");
        String defaultName = textOr(info.path("name"), "postman-import");
        String name = nonBlank(collectionName, defaultName);

        var requests = new ArrayList<ApiRequest>();
        JsonNode item = root.path("item");
        if (item.isArray()) {
            for (JsonNode n : item) {
                collectPostmanItems(n, requests);
            }
        }

        apiService.saveCollection(new Collection(name, requests, java.time.Instant.now()));
        return new ImportResult(name, requests.size(), 0);
    }

    private void collectPostmanItems(JsonNode itemNode, List<ApiRequest> out) {
        if (itemNode.has("item") && itemNode.path("item").isArray()) {
            for (JsonNode child : itemNode.path("item")) {
                collectPostmanItems(child, out);
            }
            return;
        }
        JsonNode reqNode = itemNode.path("request");
        if (!reqNode.isObject()) return;

        String methodRaw = textOr(reqNode.path("method"), "GET");
        HttpMethod method = HttpMethod.fromString(methodRaw);
        String requestName = textOr(itemNode.path("name"), method.name());
        String rawUrl = extractPostmanUrl(reqNode.path("url"));
        if (rawUrl == null || rawUrl.isBlank()) return;

        var headers = new LinkedHashMap<String, String>();
        JsonNode hdr = reqNode.path("header");
        if (hdr.isArray()) {
            for (JsonNode h : hdr) {
                String key = textOr(h.path("key"), null);
                String value = textOr(h.path("value"), null);
                if (key != null && value != null) headers.put(key, value);
            }
        }

        String body = null;
        JsonNode bodyNode = reqNode.path("body");
        if (bodyNode.isObject() && "raw".equalsIgnoreCase(textOr(bodyNode.path("mode"), ""))) {
            body = textOr(bodyNode.path("raw"), null);
        }

        var req = new ApiRequest(requestName, method, rawUrl, headers, body);
        out.add(req);
    }

    private MockRoute buildMockFromOpenApi(String path, HttpMethod method, JsonNode op) {
        int status = pickResponseStatus(op.path("responses"));
        String body = pickResponseBody(op.path("responses"));
        var headers = new LinkedHashMap<String, String>();
        headers.put("Content-Type", "application/json");
        String desc = textOr(op.path("summary"), method.name() + " " + path);
        return new MockRoute(method, path, status, headers, body, 0, 0, List.of(), desc);
    }

    private int pickResponseStatus(JsonNode responses) {
        if (!responses.isObject()) return 200;
        int fallback = 200;
        Iterator<String> it = responses.fieldNames();
        while (it.hasNext()) {
            String code = it.next();
            if ("default".equalsIgnoreCase(code)) continue;
            try {
                int parsed = Integer.parseInt(code);
                if (parsed >= 200 && parsed < 300) return parsed;
                fallback = parsed;
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private String pickResponseBody(JsonNode responses) {
        if (!responses.isObject()) return "{}";
        Iterator<String> it = responses.fieldNames();
        while (it.hasNext()) {
            String code = it.next();
            JsonNode resp = responses.path(code);
            JsonNode content = resp.path("content").path("application/json");
            JsonNode example = content.path("example");
            if (!example.isMissingNode()) {
                return example.isTextual() ? example.asText() : example.toString();
            }
        }
        return "{}";
    }

    private boolean hasJsonRequestBody(JsonNode op) {
        return op.path("requestBody").path("content").path("application/json").isObject();
    }

    private String defaultJsonBody(JsonNode op) {
        JsonNode json = op.path("requestBody").path("content").path("application/json");
        JsonNode example = json.path("example");
        if (!example.isMissingNode()) {
            return example.isTextual() ? example.asText() : example.toString();
        }
        return "{}";
    }

    private String detectOpenApiBaseUrl(JsonNode root) {
        JsonNode servers = root.path("servers");
        if (servers.isArray() && !servers.isEmpty()) {
            return textOr(servers.get(0).path("url"), null);
        }
        return "{{base_url}}";
    }

    private String extractPostmanUrl(JsonNode urlNode) {
        if (urlNode == null || urlNode.isMissingNode() || urlNode.isNull()) return null;
        if (urlNode.isTextual()) return urlNode.asText();
        String raw = textOr(urlNode.path("raw"), null);
        if (raw != null) return raw;
        JsonNode host = urlNode.path("host");
        JsonNode path = urlNode.path("path");
        if (host.isArray() && path.isArray()) {
            String h = String.join(".", toStringList(host));
            String p = String.join("/", toStringList(path));
            return "https://" + h + "/" + p;
        }
        return null;
    }

    private List<String> toStringList(JsonNode arr) {
        var out = new ArrayList<String>();
        for (JsonNode n : arr) out.add(n.asText());
        return out;
    }

    private String joinUrl(String base, String path) {
        if (base.endsWith("/") && path.startsWith("/")) return base.substring(0, base.length() - 1) + path;
        if (!base.endsWith("/") && !path.startsWith("/")) return base + "/" + path;
        return base + path;
    }

    private String textOr(JsonNode node, String fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) return fallback;
        String value = node.asText();
        return value == null || value.isBlank() ? fallback : value;
    }

    private String nonBlank(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }

    public record ImportResult(String collectionName, int importedRequests, int createdMocks) {}
}
