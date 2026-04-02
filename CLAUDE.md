# GPC BDD API Test Framework

## Project Overview
Cucumber BDD framework for testing GPC (Global Payment Company) APIs. Uses RestAssured for HTTP calls, JUnit 5 as the test runner, and PicoContainer for dependency injection.

## Tech Stack
- Java 17, Maven, Cucumber 7, JUnit 5, RestAssured 5, Jackson, PicoContainer

## Build & Run

```bash
# Run all @regression tests (default env: dev)
mvn test

# Run with a specific tag and environment
mvn test -Denv=staging -Dcucumber.filter.tags="@smoke"

# Run a single feature
mvn test -Dcucumber.filter.tags="@lending-plan-eligibility"
```

## Directory Layout

```
src/test/
├── java/
│   ├── RunCucumberTest.java          # JUnit Platform Suite entry point
│   ├── config/EnvConfig.java         # Environment-aware property loader
│   ├── constants/Endpoints.java      # API path constants
│   ├── helpers/
│   │   ├── DataHelper.java           # Loads test-data JSON from classpath
│   │   └── MockHelper.java           # Registers/clears mock interactions
│   ├── stepdefs/                     # Cucumber step definitions
│   │   ├── CommonSteps.java          # Shared steps (mock setup, assertions, store/recall)
│   │   └── ReadLending...Steps.java  # Domain-specific steps per API
│   └── support/
│       ├── Hooks.java                # @BeforeAll, @Before, @After lifecycle
│       └── ScenarioContext.java      # Per-scenario shared state (PicoContainer world object)
└── resources/
    ├── features/                     # .feature files
    ├── interactions/                  # Mock server interaction JSON (one file = one composite response)
    ├── test-data/customers/          # Customer token data per market
    ├── application-{env}.properties  # Per-environment config
    ├── junit-platform.properties     # Cucumber glue + plugin config
    └── logback-test.xml              # Logging config
```

## Conventions

### Adding a new API test
1. Add the endpoint path to `constants/Endpoints.java`.
2. Create a new `.feature` file under `src/test/resources/features/`.
3. Create a step definition class under `stepdefs/` — inject `ScenarioContext` via constructor.
4. Add mock interaction JSONs under `src/test/resources/interactions/{service-name}/`.
5. Add customer/test data under `src/test/resources/test-data/` if needed.

### Mock interactions
- Each JSON file represents one composite downstream response, not one field.
- File path convention: `interactions/{downstream-api}/{variant-description}.json`
- Interactions are registered per-scenario and cleared automatically in `@Before`.

### Feature file templates
The `read-lending-plan-eligibility.feature` contains four scenario patterns to copy from:
1. **Simple scenario** — single API call, basic assertions
2. **Scenario Outline** — data-driven with Examples table (comma-separated interactions)
3. **DataTable response validation** — assert many fields via `the response should match:` step
4. **Chained API calls** — store a value from response A, use it to verify response B

### Step definitions
- Put reusable steps in `CommonSteps.java` (mock setup, response assertions, store/recall).
- Put API-specific Given/When steps in a dedicated `{ApiName}Steps.java` class.
- Never put shared assertion logic in domain step classes.

### Test data
- Customer tokens live in `test-data/customers/{market}.json`.
- Each file must contain at minimum an `accountToken` field.

## Reports
- HTML: `target/reports/cucumber.html`
- JSON: `target/reports/cucumber.json`
- CI uploads both as artifacts (14-day retention).

## CI
GitHub Actions workflow in `.github/workflows/bdd-tests.yml` runs on push/PR to `main` and `develop`.
