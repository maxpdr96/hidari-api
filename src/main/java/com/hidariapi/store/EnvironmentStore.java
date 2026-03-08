package com.hidariapi.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hidariapi.model.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Persiste ambientes (variaveis) em {@code ~/.config/hidariapi/environments.json}.
 */
@Component
public class EnvironmentStore {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentStore.class);

    private static final Path CONFIG_DIR = Path.of(
            System.getProperty("user.home"), ".config", "hidariapi");
    private static final Path FILE = CONFIG_DIR.resolve("environments.json");

    private final ObjectMapper mapper;
    private LinkedHashMap<String, Environment> environments;

    public EnvironmentStore() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.environments = load();
    }

    /** Salva ou atualiza um ambiente. */
    public void save(Environment env) {
        environments.put(env.name(), env);
        persist();
    }

    /** Remove um ambiente pelo nome. */
    public boolean remove(String name) {
        boolean existed = environments.remove(name) != null;
        if (existed) persist();
        return existed;
    }

    /** Retorna todos os ambientes. */
    public List<Environment> list() {
        return new ArrayList<>(environments.values());
    }

    /** Retorna ambiente pelo nome. */
    public Optional<Environment> get(String name) {
        return Optional.ofNullable(environments.get(name));
    }

    public int size() { return environments.size(); }

    private LinkedHashMap<String, Environment> load() {
        var result = new LinkedHashMap<String, Environment>();
        if (!Files.exists(FILE)) return result;
        try {
            List<Environment> list = mapper.readValue(
                    FILE.toFile(), new TypeReference<List<Environment>>() {});
            list.forEach(e -> result.put(e.name(), e));
            log.debug("Loaded {} environments", result.size());
        } catch (IOException e) {
            log.warn("Could not load environments: {}", e.getMessage());
        }
        return result;
    }

    private void persist() {
        try {
            Files.createDirectories(CONFIG_DIR);
            mapper.writeValue(FILE.toFile(), new ArrayList<>(environments.values()));
        } catch (IOException e) {
            log.warn("Could not save environments: {}", e.getMessage());
        }
    }
}
