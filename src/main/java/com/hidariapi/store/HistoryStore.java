package com.hidariapi.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hidariapi.model.SavedRequest;
import com.hidariapi.util.AppPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Persiste historico de requests no diretório de config da aplicação.
 * Mantém no maximo {@code maxHistory} entradas (FIFO).
 */
@Component
public class HistoryStore {

    private static final Logger log = LoggerFactory.getLogger(HistoryStore.class);

    private static final Path CONFIG_DIR = AppPaths.configDir();
    private static final Path FILE = CONFIG_DIR.resolve("history.json");

    private final ObjectMapper mapper;
    private final int maxHistory;
    private LinkedList<SavedRequest> history;

    public HistoryStore(@Value("${hidariapi.max-history:200}") int maxHistory) {
        this.maxHistory = maxHistory;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.history = load();
    }

    /** Adiciona ao historico (mais recente no inicio). */
    public void add(SavedRequest entry) {
        history.addFirst(entry);
        while (history.size() > maxHistory) {
            history.removeLast();
        }
        persist();
    }

    /** Retorna todo o historico (mais recente primeiro). */
    public List<SavedRequest> list() {
        return new ArrayList<>(history);
    }

    /** Retorna os ultimos N registros. */
    public List<SavedRequest> last(int n) {
        return history.stream().limit(n).toList();
    }

    /** Retorna entrada por indice (1-based). */
    public SavedRequest get(int index) {
        if (index < 1 || index > history.size()) return null;
        return history.get(index - 1);
    }

    /** Limpa todo o historico. */
    public void clear() {
        history.clear();
        persist();
    }

    public int size() { return history.size(); }

    private LinkedList<SavedRequest> load() {
        var result = new LinkedList<SavedRequest>();
        if (!Files.exists(FILE)) return result;
        try {
            List<SavedRequest> list = mapper.readValue(
                    FILE.toFile(), new TypeReference<List<SavedRequest>>() {});
            result.addAll(list);
            log.debug("Loaded {} history entries", result.size());
        } catch (IOException e) {
            log.warn("Could not load history: {}", e.getMessage());
        }
        return result;
    }

    private void persist() {
        try {
            Files.createDirectories(CONFIG_DIR);
            mapper.writeValue(FILE.toFile(), new ArrayList<>(history));
        } catch (IOException e) {
            log.warn("Could not save history: {}", e.getMessage());
        }
    }
}
