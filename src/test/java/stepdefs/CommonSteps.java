package stepdefs;

import helpers.MockHelper;
import helpers.ResponseValidator;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import support.ScenarioContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared steps reusable across all feature files.
 *
 * Guidelines:
 *   - Only business-readable steps belong here.
 *   - No JSON field paths, HTTP methods, or endpoint URLs in step text.
 *   - Technical details (endpoints, payloads, field extraction) live
 *     in step definition code, NOT in the feature file.
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

    // ── Response assertions ─────────────────────────────────────────────

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatus) {
        assertNotNull(context.getResponse(), "No response captured — was the API called?");
        assertEquals(expectedStatus, context.getResponse().getStatusCode(),
                "Unexpected HTTP status code");
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

    // ── Named-expectation validation ────────────────────────────────────

    @Then("the response body should match expectation {string}")
    public void theResponseBodyShouldMatchExpectation(String expectationName) {
        assertNotNull(context.getResponse(), "No response captured — was the API called?");
        List<String> mismatches = ResponseValidator.validate(
                context.getResponse().getBody().asString(), expectationName);
        assertTrue(mismatches.isEmpty(),
                "Response did not match expectation '" + expectationName + "':\n  - "
                        + String.join("\n  - ", mismatches));
    }
}
