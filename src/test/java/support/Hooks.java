package support;

import config.EnvConfig;
import helpers.MockHelper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Scenario;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;

import java.io.File;
import java.util.UUID;

public class Hooks {

    private final ScenarioContext context;

    public Hooks(ScenarioContext context) {
        this.context = context;
    }

    @BeforeAll
    public static void globalSetup() {
        // Ensure reports directory exists
        new File("target/reports").mkdirs();

        // Configure RestAssured defaults
        RestAssured.baseURI = EnvConfig.baseUrl();
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("x-api-key", EnvConfig.apiKey())
                .build();
        RestAssured.config = RestAssured.config()
                .httpClient(
                        RestAssured.config().getHttpClientConfig()
                                .setParam("http.connection.timeout", EnvConfig.requestTimeout())
                                .setParam("http.socket.timeout", EnvConfig.requestTimeout())
                );
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }

    @Before
    public void scenarioSetup() {
        // Clear all mock interactions
        MockHelper.clearAllInteractions();

        // Reset scenario context
        context.reset();

        // Generate a unique correlation ID for this scenario
        context.setCorrelationId(UUID.randomUUID().toString());
    }

    @After
    public void scenarioTeardown(Scenario scenario) {
        if (scenario.isFailed() && context.getResponse() != null) {
            String body = context.getResponse().getBody().asPrettyString();
            scenario.attach(body, "application/json", "Response Body");
        }
    }
}
