package org.common.db.config;

import java.util.Properties;

public class ConfigLoader {
    public static Properties properties = StubServerConfigManager.load(System.getProperty("config.name", "config_local.properties"));

    public static String getProperty(String key) {
        // get the property value and print it out
        return properties.getProperty(key);
    }
}
