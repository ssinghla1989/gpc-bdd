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
│   │   ├── MockHelper.java           # Registers/clears mock interactions
│   │   └── ResponseValidator.java    # JSON expectation matcher engine
│   ├── stepdefs/                     # Cucumber step definitions
│   │   ├── CommonSteps.java          # Shared steps (mocks, assertions, expectations)
│   │   └── ReadLending...Steps.java  # Domain-specific steps per API
│   └── support/
│       ├── Hooks.java                # @BeforeAll, @Before, @After lifecycle
│       └── ScenarioContext.java      # Per-scenario shared state (PicoContainer world object)
└── resources/
    ├── features/                     # .feature files
    ├── expectations/                 # Expected response JSON (with matchers)
    ├── interactions/                 # Mock server interaction JSON
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
5. Add expectation JSONs under `src/test/resources/expectations/{api-name}/` for complex responses.
6. Add customer/test data under `src/test/resources/test-data/` if needed.

### Mock interactions
- Each JSON file represents one composite downstream response, not one field.
- File path convention: `interactions/{downstream-api}/{variant-description}.json`
- Interactions are registered per-scenario and cleared automatically in `@Before`.

### Response validation — choosing the right approach
- **Inline field check** (`the response field "x" should be "y"`) — use for smoke tests or when you only care about 1–2 fields.
- **Named expectation** (`the response body should match expectation "..."`) — use for anything with complex structure, nested objects, arrays, or more than a couple of fields. This keeps the feature file focused on business intent.
- **Never** put a wall of field-by-field assertions in the feature file — that belongs in an expectation JSON.

### Expectation files and matchers
Expectation files live under `src/test/resources/expectations/{api-name}/{variant}.json`.
They are regular JSON with optional matchers as string values:

| Matcher              | Meaning                                     |
|----------------------|---------------------------------------------|
| `${ignore}`          | Field must exist; value is not checked       |
| `${notNull}`         | Field must exist and not be JSON null         |
| `${type:string}`     | Type check (also: `number`, `boolean`, `array`, `object`) |
| `${regex:pattern}`   | Value must match the Java regex pattern       |
| `${contains:text}`   | Value must contain the substring              |
| `${greaterThan:n}`   | Value must be a number greater than n         |

Any value that is not a matcher is compared literally.
Matching is **partial** — the actual response may have extra fields not in the expectation; only declared fields are checked.

Example:
```json
{
  "eligible": true,
  "market": "US",
  "maxInstallments": "${type:number}",
  "lendingConfig": {
    "provider": "${notNull}",
    "evaluatedAt": "${regex:\\d{4}-\\d{2}-\\d{2}.*}"
  }
}
```

### Feature file templates
The `read-lending-plan-eligibility.feature` contains four scenario patterns:
1. **Simple scenario** — single API call, 1–2 inline field assertions
2. **Scenario Outline** — data-driven with Examples table
3. **Named expectation** — complex response validated against an expectation JSON
4. **Chained API calls** — store a value from response A, verify it against response B

### Step definitions
- Put reusable steps in `CommonSteps.java` (mock setup, assertions, expectations, store/recall).
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
