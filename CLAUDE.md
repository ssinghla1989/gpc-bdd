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
- **Domain assertion** (`the customer should be eligible`) — preferred. Encapsulates status code + field checks in the step definition. Feature file stays business-readable.
- **Named expectation** (`the response body should match expectation "..."`) — use for complex responses with many fields, nested objects, or arrays. The JSON expectation file defines structure and matchers.
- **Never** put JSON field paths, HTTP details, or endpoint URLs in the feature file — those belong in step definitions.

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
1. **Simple scenario** — single API call, domain assertion (`the customer should be eligible`)
2. **Scenario Outline** — data-driven with Examples table
3. **Named expectation** — complex response validated against an expectation JSON
4. **Chained API calls** — call API A then API B, assert business consistency across both

### What belongs WHERE

| In the feature file                  | In the step definition               |
|--------------------------------------|--------------------------------------|
| `the customer should be eligible`    | `assertEquals(200, ...statusCode())` |
| `eligibility for feature "X"`       | `.post(Endpoints.READ_...)`          |
| `lending config for market "US"`     | `.body(Map.of("market", market))`    |
| `should agree on installment limits` | `getInt("maxInstallments")`          |

### Step definitions
- **CommonSteps** — only generic, reusable steps (mock setup, status code, named expectations).
- **{ApiName}Steps** — all domain-specific logic: API calls, request construction, business assertions, cross-call comparisons.
- Never expose JSON field names, endpoints, or HTTP methods in feature file step text.

### Test data
- Customer tokens live in `test-data/customers/{market}.json`.
- Each file must contain at minimum an `accountToken` field.

## Reports
- HTML: `target/reports/cucumber.html`
- JSON: `target/reports/cucumber.json`
- CI uploads both as artifacts (14-day retention).

## CI
GitHub Actions workflow in `.github/workflows/bdd-tests.yml` runs on push/PR to `main` and `develop`.
