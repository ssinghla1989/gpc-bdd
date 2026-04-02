@regression @lending-plan-eligibility
Feature: ReadLendingPlanEligibility API
  As a lending service consumer
  I want to check customer eligibility for Plan It features
  So that I can offer the correct lending products based on market rules

  # ── Template 1: Simple happy-path ─────────────────────────────────────
  #    Quick smoke test — one or two inline field checks prove the flow works.
  #    Best for: smoke tests, minimal-confidence checks.

  @smoke
  Scenario: US active basic-card customer is eligible for Plan It Amount
    Given the following mock interactions are registered:
      | interaction                              |
      | read-member/us-active-basic              |
      | read-lending-config/us-plan-it-enabled   |
    And a "us" market customer
    When the customer checks eligibility for feature "PLAN-IT-AMOUNT"
    Then the response status code should be 200
    And the response field "eligible" should be "true"

  # ── Template 2: Data-driven error cases (Scenario Outline) ────────────
  #    Same test logic, parameterised across rows.
  #    Best for: covering many negative paths without duplicating scenarios.

  Scenario Outline: Decline — <reason>
    Given mock interactions "<interactions>" are registered
    And a "<market>" market customer
    When the customer checks eligibility for feature "<feature>"
    Then the request should be declined with status <status> and error "<error>"

    Examples:
      | reason                    | market | interactions                                                               | feature         | status | error                                       |
      | Japan market ineligible   | japan  | read-member/japan-market-response                                          | PLAN-IT-AMOUNT  | 422    | Feature name is invalid for japan market     |
      | Inactive account          | us     | read-member/us-inactive-basic                                              | PLAN-IT-AMOUNT  | 422    | Account is not active                        |
      | Supplementary card holder | us     | read-member/us-active-supplementary,read-lending-config/us-plan-it-enabled | PLAN-IT-AMOUNT  | 422    | Supplementary card holders are not eligible  |
      | Invalid feature name      | us     | read-member/us-active-basic                                                | INVALID-FEATURE | 422    | Feature name is invalid                      |

  # ── Template 3: Named expectation for complex response validation ─────
  #    The feature file states WHAT to validate; the HOW lives in a JSON
  #    file under src/test/resources/expectations/.
  #    The expectation JSON supports matchers: ${notNull}, ${type:number},
  #    ${regex:pattern}, ${contains:text}, ${greaterThan:n}, ${ignore}.
  #    Best for: responses with many fields, nested objects, or arrays.

  Scenario: Eligible response contains full lending configuration
    Given the following mock interactions are registered:
      | interaction                              |
      | read-member/us-active-basic              |
      | read-lending-config/us-plan-it-enabled   |
    And a "us" market customer
    When the customer checks eligibility for feature "PLAN-IT-AMOUNT"
    Then the response status code should be 200
    And the response body should match expectation "read-lending-plan-eligibility/us-eligible-plan-it-amount"

  # ── Template 4: Chained API calls with cross-call validation ──────────
  #    Call API A, store a field, call API B, verify stored value.
  #    Best for: workflows where APIs feed into each other.

  Scenario: Eligibility details are consistent with lending configuration
    Given the following mock interactions are registered:
      | interaction                              |
      | read-member/us-active-basic              |
      | read-lending-config/us-plan-it-enabled   |
    And a "us" market customer
    When the customer checks eligibility for feature "PLAN-IT-AMOUNT"
    Then the response status code should be 200
    And I store the response field "maxInstallments" as "planMaxInstallments"
    When I call POST "/ReadLendingConfig.v1" with body:
      """
      { "market": "US" }
      """
    Then the response status code should be 200
    And the response field "maxInstallments" should equal stored value "planMaxInstallments"
