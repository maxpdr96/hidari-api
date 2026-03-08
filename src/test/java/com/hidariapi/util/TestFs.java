package com.hidariapi.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TestFs {

    private TestFs() {}

    public static void deleteRecursively(Path path) throws IOException {
        if (path == null || !Files.exists(path)) return;
        try (var stream = Files.walk(path)) {
            stream.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
