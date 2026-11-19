package com.sentinel.util;

import java.io.*;
import java.nio.file.*;

public record SettingsBackup(String reportFrom, String reportTo) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    public void save(Path directory) throws IOException {
        Path file = directory.resolve("settings.ser");
        Path temporary = Files.createTempFile(directory, "settings-", ".tmp");
        try {
            try (var output = new ObjectOutputStream(Files.newOutputStream(temporary))) {
                output.writeObject(this);
            }
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static SettingsBackup load(Path directory) throws IOException {
        Path file = directory.resolve("settings.ser");
        if (!Files.exists(file)) return null;
        try (var input = new ObjectInputStream(Files.newInputStream(file))) {
            input.setObjectInputFilter(info -> {
                if (info.depth() > 2 || info.streamBytes() > 4096 || info.references() > 8) {
                    return ObjectInputFilter.Status.REJECTED;
                }
                Class<?> type = info.serialClass();
                return type == null || type == SettingsBackup.class || type == String.class
                        ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
            });
            if (input.readObject() instanceof SettingsBackup settings) return settings;
            throw new IOException("Unexpected settings file format");
        } catch (ClassNotFoundException error) {
            throw new IOException("Settings class could not be read", error);
        }
    }
}
