package helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility for loading test data JSON files from the classpath.
 *
 * Conventions:
 *   - Customer tokens:  test-data/customers/{market}.json
 *   - API payloads:     test-data/{api-name}/{file}.json
 */
public final class DataHelper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private DataHelper() {
    }

    /**
     * Loads a customer token file for the given market.
     */
    public static JsonNode loadCustomerData(String market) {
        return loadJson("test-data/customers/" + market + ".json");
    }

    /**
     * Loads raw JSON string from the classpath.
     */
    public static String loadRawJson(String classpathResource) {
        try (InputStream is = DataHelper.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (is == null) {
                throw new IllegalArgumentException("Test data file not found: " + classpathResource);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read test data: " + classpathResource, e);
        }
    }

    /**
     * Loads and parses a JSON file from the classpath.
     */
    public static JsonNode loadJson(String classpathResource) {
        try {
            return objectMapper.readTree(loadRawJson(classpathResource));
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON: " + classpathResource, e);
        }
    }
}
