package stepdefs;

import com.fasterxml.jackson.databind.JsonNode;
import constants.Endpoints;
import helpers.DataHelper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import support.ScenarioContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Steps specific to the ReadLendingPlanEligibility API.
 *
 * All HTTP details — endpoints, payloads, field extraction — are
 * encapsulated here so the feature file reads like a business spec.
 */
public class ReadLendingPlanEligibilitySteps {

    private final ScenarioContext context;

    public ReadLendingPlanEligibilitySteps(ScenarioContext context) {
        this.context = context;
    }

    // ── Given ───────────────────────────────────────────────────────────

    @Given("a {string} market customer")
    public void aMarketCustomer(String market) {
        JsonNode customer = DataHelper.loadCustomerData(market);
        JsonNode tokenNode = customer.get("accountToken");
        assertNotNull(tokenNode,
                "Customer data for market '" + market + "' is missing required field 'accountToken'");
        context.setAccountToken(tokenNode.asText());
    }

    // ── When ────────────────────────────────────────────────────────────

    @When("the customer checks eligibility for feature {string}")
    public void theCustomerChecksEligibilityForFeature(String featureName) {
        Response response = RestAssured.given()
                .header("correlation-id", context.getCorrelationId())
                .body(Map.of(
                        "accountToken", context.getAccountToken(),
                        "featureName", featureName))
                .post(Endpoints.READ_LENDING_PLAN_ELIGIBILITY);

        context.setResponse(response);
    }

    @When("the lending configuration is retrieved for market {string}")
    public void theLendingConfigurationIsRetrievedForMarket(String market) {
        // Preserve the eligibility response for cross-call comparison
        context.put("eligibilityResponse", context.getResponse());

        Response response = RestAssured.given()
                .header("correlation-id", context.getCorrelationId())
                .body(Map.of("market", market))
                .post(Endpoints.READ_LENDING_CONFIG);

        context.setResponse(response);
    }

    // ── Then ────────────────────────────────────────────────────────────

    @Then("the customer should be eligible")
    public void theCustomerShouldBeEligible() {
        assertNotNull(context.getResponse(), "No response captured — was the API called?");
        assertEquals(200, context.getResponse().getStatusCode(),
                "Expected HTTP 200 for eligible customer");
        assertEquals("true", context.getResponse().jsonPath().getString("eligible"),
                "Expected 'eligible' to be true");
    }

    @Then("the eligibility and lending configuration should agree on installment limits")
    public void eligibilityAndConfigShouldAgreeOnInstallmentLimits() {
        Response configResponse = context.getResponse();
        Response eligibilityResponse = context.get("eligibilityResponse");

        assertNotNull(eligibilityResponse, "No eligibility response stored — was the eligibility API called first?");
        assertNotNull(configResponse, "No config response captured — was the config API called?");

        int eligibilityMax = eligibilityResponse.jsonPath().getInt("maxInstallments");
        int configMax = configResponse.jsonPath().getInt("maxInstallments");

        assertEquals(configMax, eligibilityMax,
                "Eligibility maxInstallments should match lending configuration");
    }
}
