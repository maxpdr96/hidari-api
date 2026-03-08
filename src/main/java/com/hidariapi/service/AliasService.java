package com.hidariapi.service;

import com.hidariapi.model.CommandAlias;
import com.hidariapi.store.AliasStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AliasService {

    private final AliasStore aliasStore;

    public AliasService(AliasStore aliasStore) {
        this.aliasStore = aliasStore;
    }

    public void save(String name, String command) {
        aliasStore.save(CommandAlias.of(normalizeName(name), command.trim()));
    }

    public boolean remove(String name) {
        return aliasStore.remove(normalizeName(name));
    }

    public Optional<CommandAlias> get(String name) {
        return aliasStore.get(normalizeName(name));
    }

    public List<CommandAlias> list() {
        return aliasStore.list();
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }
}
