package stepdefs;

import com.fasterxml.jackson.databind.JsonNode;
import constants.Endpoints;
import helpers.DataHelper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import support.ScenarioContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ReadLendingPlanEligibilitySteps {

    private final ScenarioContext context;

    public ReadLendingPlanEligibilitySteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("a {string} market customer")
    public void aMarketCustomer(String market) {
        JsonNode customer = DataHelper.loadCustomerData(market);
        JsonNode tokenNode = customer.get("accountToken");
        assertNotNull(tokenNode,
                "Customer data for market '" + market + "' is missing required field 'accountToken'");
        context.setAccountToken(tokenNode.asText());
    }

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
}
