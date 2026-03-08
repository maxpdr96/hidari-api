package com.hidariapi.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hidariapi.model.Collection;
import com.hidariapi.util.AppPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Persiste colecoes de requests no diretório de config da aplicação.
 */
@Component
public class CollectionStore {

    private static final Logger log = LoggerFactory.getLogger(CollectionStore.class);

    private static final Path CONFIG_DIR = AppPaths.configDir();
    private static final Path FILE = CONFIG_DIR.resolve("collections.json");

    private final ObjectMapper mapper;
    private LinkedHashMap<String, Collection> collections;

    public CollectionStore() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.collections = load();
    }

    /** Cria ou atualiza uma colecao. */
    public void save(Collection collection) {
        collections.put(collection.name(), collection);
        persist();
    }

    /** Remove uma colecao pelo nome. */
    public boolean remove(String name) {
        boolean existed = collections.remove(name) != null;
        if (existed) persist();
        return existed;
    }

    /** Retorna todas as colecoes. */
    public List<Collection> list() {
        return new ArrayList<>(collections.values());
    }

    /** Retorna colecao pelo nome. */
    public Optional<Collection> get(String name) {
        return Optional.ofNullable(collections.get(name));
    }

    /** Retorna colecao por indice (1-based). */
    public Optional<Collection> getByIndex(int index) {
        if (index < 1 || index > collections.size()) return Optional.empty();
        return Optional.of(new ArrayList<>(collections.values()).get(index - 1));
    }

    public int size() { return collections.size(); }

    private LinkedHashMap<String, Collection> load() {
        var result = new LinkedHashMap<String, Collection>();
        if (!Files.exists(FILE)) return result;
        try {
            List<Collection> list = mapper.readValue(
                    FILE.toFile(), new TypeReference<List<Collection>>() {});
            list.forEach(c -> result.put(c.name(), c));
            log.debug("Loaded {} collections", result.size());
        } catch (IOException e) {
            log.warn("Could not load collections: {}", e.getMessage());
        }
        return result;
    }

    private void persist() {
        try {
            Files.createDirectories(CONFIG_DIR);
            mapper.writeValue(FILE.toFile(), new ArrayList<>(collections.values()));
        } catch (IOException e) {
            log.warn("Could not save collections: {}", e.getMessage());
        }
    }
}
