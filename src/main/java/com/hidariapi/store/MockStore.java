package com.hidariapi.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hidariapi.model.MockRoute;
import com.hidariapi.util.AppPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persiste rotas do mock server no diretório de config da aplicação.
 */
@Component
public class MockStore {

    private static final Logger log = LoggerFactory.getLogger(MockStore.class);

    private static final Path CONFIG_DIR = AppPaths.configDir();
    private static final Path FILE = CONFIG_DIR.resolve("mocks.json");

    private final ObjectMapper mapper;
    private List<MockRoute> routes;

    public MockStore() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.routes = load();
    }

    /** Adiciona uma rota. Se ja existe rota com mesmo method+path, substitui. */
    public void add(MockRoute route) {
        routes.removeIf(r -> r.routeKey().equals(route.routeKey()));
        routes.add(route);
        persist();
    }

    /** Remove rota por indice (1-based). */
    public boolean remove(int index) {
        if (index < 1 || index > routes.size()) return false;
        routes.remove(index - 1);
        persist();
        return true;
    }

    /** Remove rota por method + path. */
    public boolean remove(String method, String path) {
        var key = method.toUpperCase() + " " + path;
        boolean removed = routes.removeIf(r -> r.routeKey().equals(key));
        if (removed) persist();
        return removed;
    }

    /** Retorna todas as rotas. */
    public List<MockRoute> list() {
        return new ArrayList<>(routes);
    }

    /** Retorna rota por indice (1-based). */
    public Optional<MockRoute> get(int index) {
        if (index < 1 || index > routes.size()) return Optional.empty();
        return Optional.of(routes.get(index - 1));
    }

    /** Encontra a primeira rota que bate com method + path. */
    public Optional<MockRoute> findMatch(String method, String path) {
        return routes.stream()
                .filter(r -> r.matches(method, path))
                .findFirst();
    }

    /** Atualiza rota por indice (1-based). */
    public boolean update(int index, MockRoute route) {
        if (index < 1 || index > routes.size()) return false;
        routes.set(index - 1, route);
        persist();
        return true;
    }

    /** Limpa todas as rotas. */
    public void clear() {
        routes.clear();
        persist();
    }

    public int size() { return routes.size(); }

    private List<MockRoute> load() {
        var result = new ArrayList<MockRoute>();
        if (!Files.exists(FILE)) return result;
        try {
            result.addAll(mapper.readValue(FILE.toFile(), new TypeReference<List<MockRoute>>() {}));
            log.debug("Loaded {} mock routes", result.size());
        } catch (IOException e) {
            log.warn("Could not load mocks: {}", e.getMessage());
        }
        return result;
    }

    private void persist() {
        try {
            Files.createDirectories(CONFIG_DIR);
            mapper.writeValue(FILE.toFile(), routes);
        } catch (IOException e) {
            log.warn("Could not save mocks: {}", e.getMessage());
        }
    }
}
