package org.common.db.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class StubServerConfigManager {

    private static final String APP_DIR = "stubserver_config/server";
    private static final String CONFIG_FILE = "config.properties";
    private static final String DEFAULT_CONFIG = "config_local.properties";

    public static Properties loadResource() throws IOException {
        String resourceName = System.getProperty("config.name", DEFAULT_CONFIG);
        InputStream s = StubServerConfigManager.class.getClassLoader().getResourceAsStream(resourceName);
        Properties properties = new Properties();
        properties.load(s);
        return properties;
    }

    public static Properties load(String configName) {
        try {
            if (true) {
                return loadResource();
            }

            Path userDir = Paths.get(System.getProperty("user.home"), APP_DIR);
            Path userConfig = userDir.resolve(CONFIG_FILE);

            // First run: copy default config
            if (Files.notExists(userConfig)) {
                Files.createDirectories(userDir);
                copyFromClasspath(configName, userConfig);
            }

            // Always load from user directory
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(userConfig)) {
                props.load(in);
            }
            return props;

        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize StubServer config", e);
        }
    }

    private static void copyFromClasspath(String resource, Path target) throws IOException {
        try (InputStream in = StubServerConfigManager.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new FileNotFoundException("Classpath resource not found: " + resource);
            }
            Files.copy(in, target);
        }
    }
}
