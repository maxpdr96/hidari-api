package com.hidariapi.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hidariapi.model.CommandAlias;
import com.hidariapi.util.AppPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Persists user aliases in config directory.
 */
@Component
public class AliasStore {

    private static final Logger log = LoggerFactory.getLogger(AliasStore.class);

    private static final Path CONFIG_DIR = AppPaths.configDir();
    private static final Path FILE = CONFIG_DIR.resolve("aliases.json");

    private final ObjectMapper mapper;
    private final LinkedHashMap<String, CommandAlias> aliases;

    public AliasStore() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.aliases = load();
    }

    public void save(CommandAlias alias) {
        aliases.put(alias.name(), alias);
        persist();
    }

    public boolean remove(String name) {
        boolean removed = aliases.remove(name) != null;
        if (removed) persist();
        return removed;
    }

    public Optional<CommandAlias> get(String name) {
        return Optional.ofNullable(aliases.get(name));
    }

    public List<CommandAlias> list() {
        return aliases.values().stream()
                .sorted(Comparator.comparing(CommandAlias::name))
                .toList();
    }

    private LinkedHashMap<String, CommandAlias> load() {
        var out = new LinkedHashMap<String, CommandAlias>();
        if (!Files.exists(FILE)) return out;
        try {
            List<CommandAlias> list = mapper.readValue(FILE.toFile(), new TypeReference<List<CommandAlias>>() {});
            for (var alias : list) {
                out.put(alias.name(), alias);
            }
        } catch (IOException e) {
            log.warn("Could not load aliases: {}", e.getMessage());
        }
        return out;
    }

    private void persist() {
        try {
            Files.createDirectories(CONFIG_DIR);
            mapper.writeValue(FILE.toFile(), new ArrayList<>(aliases.values()));
        } catch (IOException e) {
            log.warn("Could not save aliases: {}", e.getMessage());
        }
    }
}
