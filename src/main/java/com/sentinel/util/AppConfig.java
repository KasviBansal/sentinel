package com.sentinel.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public record AppConfig(Path directory, Path database, LocalDate demoDate) {
    public static AppConfig load() throws IOException {
        Properties properties = new Properties();
        try (var input = AppConfig.class.getResourceAsStream("/config.properties")) {
            if (input == null) throw new IOException("config.properties is missing");
            properties.load(input);
        }
        Path directory = Path.of(System.getProperty("sentinel.data.dir", properties.getProperty("data.directory")));
        Files.createDirectories(directory);
        FileHandler handler = new FileHandler(directory.resolve("sentinel.log").toString().replace("%", "%%"), true);
        handler.setFormatter(new SimpleFormatter());
        Logger.getLogger("com.sentinel").addHandler(handler);
        return new AppConfig(directory, directory.resolve(properties.getProperty("database.file")),
                LocalDate.parse(properties.getProperty("demo.date")));
    }
}
