package support;

import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * World object injected by PicoContainer into every step definition class.
 * Holds shared state for a single scenario.
 */
public class ScenarioContext {

    private Response response;
    private String accountToken;
    private String correlationId;
    private final Map<String, Object> store = new HashMap<>();

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public String getAccountToken() {
        return accountToken;
    }

    public void setAccountToken(String accountToken) {
        this.accountToken = accountToken;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public void put(String key, Object value) {
        store.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) store.get(key);
    }

    public void reset() {
        response = null;
        accountToken = null;
        correlationId = null;
        store.clear();
    }
}
