package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class EnvConfig {

    private static final Properties properties = new Properties();
    private static final String DEFAULT_ENV = "dev";

    static {
        String env = System.getProperty("env", DEFAULT_ENV);
        String fileName = "application-" + env + ".properties";
        try (InputStream is = EnvConfig.class.getClassLoader().getResourceAsStream(fileName)) {
            if (is == null) {
                throw new IllegalStateException("Config file not found: " + fileName);
            }
            properties.load(is);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config: " + fileName, e);
        }
    }

    private EnvConfig() {
    }

    public static String baseUrl() {
        return properties.getProperty("base.url");
    }

    public static String apiKey() {
        return properties.getProperty("api.key");
    }

    public static int requestTimeout() {
        return Integer.parseInt(properties.getProperty("request.timeout"));
    }

    public static String mockServerBaseUrl() {
        return properties.getProperty("mock.server.base.url");
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }
}
