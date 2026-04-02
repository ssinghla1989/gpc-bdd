package stepdefs;

import helpers.MockHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import support.ScenarioContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Shared steps reusable across all feature files.
 */
public class CommonSteps {

    private final ScenarioContext context;

    public CommonSteps(ScenarioContext context) {
        this.context = context;
    }

    // ── Mock setup ──────────────────────────────────────────────────────

    @Given("the following mock interactions are registered:")
    public void theFollowingMockInteractionsAreRegistered(DataTable table) {
        table.asMaps().stream()
                .map(row -> row.get("interaction"))
                .forEach(MockHelper::registerInteraction);
    }

    @Given("mock interactions {string} are registered")
    public void mockInteractionsAreRegistered(String commaDelimited) {
        for (String interaction : commaDelimited.split(",")) {
            MockHelper.registerInteraction(interaction.trim());
        }
    }

    // ── Generic API calls (for chaining scenarios) ──────────────────────

    @When("I call POST {string} with body:")
    public void iCallPostWithBody(String path, String body) {
        Response response = RestAssured.given()
                .header("correlation-id", context.getCorrelationId())
                .body(body)
                .post(path);
        context.setResponse(response);
    }

    // ── Response assertions ─────────────────────────────────────────────

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatus) {
        assertNotNull(context.getResponse(), "No response captured — was the API called?");
        assertEquals(expectedStatus, context.getResponse().getStatusCode(),
                "Unexpected HTTP status code");
    }

    @Then("the response should contain field {string}")
    public void theResponseShouldContainField(String fieldPath) {
        assertNotNull(context.getResponse(), "No response captured — was the API called?");
        String value = context.getResponse().jsonPath().getString(fieldPath);
        assertNotNull(value, "Expected field '" + fieldPath + "' to be present but was null");
        assertFalse(value.isEmpty(), "Expected field '" + fieldPath + "' to be non-empty");
    }

    @Then("the response field {string} should be {string}")
    public void theResponseFieldShouldBe(String fieldPath, String expectedValue) {
        assertNotNull(context.getResponse(), "No response captured — was the API called?");
        String actual = context.getResponse().jsonPath().getString(fieldPath);
        assertEquals(expectedValue, actual,
                "Mismatch on field '" + fieldPath + "'");
    }

    @Then("the request should be declined with status {int} and error {string}")
    public void theRequestShouldBeDeclinedWithStatusAndError(int expectedStatus, String expectedMessage) {
        assertNotNull(context.getResponse(), "No response captured — was the API called?");
        assertEquals(expectedStatus, context.getResponse().getStatusCode(),
                "Unexpected HTTP status code");
        String actualMessage = context.getResponse().jsonPath().getString("error.message");
        assertEquals(expectedMessage, actualMessage,
                "Unexpected error message");
    }

    @Then("the response should match:")
    public void theResponseShouldMatch(DataTable table) {
        assertNotNull(context.getResponse(), "No response captured — was the API called?");
        for (var row : table.asMaps()) {
            String field = row.get("field");
            String expected = row.get("value");
            String actual = context.getResponse().jsonPath().getString(field);
            assertEquals(expected, actual, "Mismatch on field '" + field + "'");
        }
    }

    // ── Store & recall (for chained scenarios) ──────────────────────────

    @Then("I store the response field {string} as {string}")
    public void iStoreTheResponseField(String fieldPath, String key) {
        assertNotNull(context.getResponse(), "No response captured — was the API called?");
        String value = context.getResponse().jsonPath().getString(fieldPath);
        assertNotNull(value, "Cannot store null value from field '" + fieldPath + "'");
        context.put(key, value);
    }

    @Then("the response field {string} should equal stored value {string}")
    public void theResponseFieldShouldEqualStoredValue(String fieldPath, String key) {
        assertNotNull(context.getResponse(), "No response captured — was the API called?");
        String actual = context.getResponse().jsonPath().getString(fieldPath);
        String expected = context.get(key);
        assertNotNull(expected, "No stored value found for key '" + key + "'");
        assertEquals(expected, actual,
                "Field '" + fieldPath + "' does not match stored value '" + key + "'");
    }
}
