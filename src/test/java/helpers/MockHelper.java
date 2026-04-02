package helpers;

import config.EnvConfig;
import constants.Endpoints;
import io.restassured.RestAssured;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Communicates with the external mock server over its REST management API.
 * Interaction JSON files live under src/test/resources/interactions/{service}/{variant}.json
 */
public final class MockHelper {

    private static final Logger log = LoggerFactory.getLogger(MockHelper.class);

    private MockHelper() {
    }

    /**
     * Loads an interaction JSON from the classpath and POSTs it to the mock server.
     * <p>
     * The interaction name maps directly to a file under
     * {@code src/test/resources/interactions/<name>.json}.
     * For example, {@code "read-member/japan-market-response"} loads
     * {@code interactions/read-member/japan-market-response.json}.
     *
     * @param interactionName path relative to interactions/, without .json extension
     */
    public static void registerInteraction(String interactionName) {
        String path = "interactions/" + interactionName + ".json";
        String payload = loadResource(path);

        RestAssured.given()
                .baseUri(EnvConfig.mockServerBaseUrl())
                .header("Content-Type", "application/json")
                .body(payload)
                .post(Endpoints.MOCK_INTERACTIONS)
                .then()
                .statusCode(200);

        log.info("Registered mock interaction: {}", interactionName);
    }

    /**
     * Deletes all interactions from the mock server.
     */
    public static void clearAllInteractions() {
        RestAssured.given()
                .baseUri(EnvConfig.mockServerBaseUrl())
                .delete(Endpoints.MOCK_INTERACTIONS)
                .then()
                .statusCode(200);

        log.debug("Cleared all mock interactions");
    }

    private static String loadResource(String path) {
        try (InputStream is = MockHelper.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Interaction file not found on classpath: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read interaction file: " + path, e);
        }
    }
}
