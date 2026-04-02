@regression @lending-plan-eligibility
Feature: ReadLendingPlanEligibility API
  As a lending service consumer
  I want to check customer eligibility for Plan It features
  So that I can offer the correct lending products based on market rules

  # ── Template 1: Simple happy-path ─────────────────────────────────────
  #    Business-level Given/When/Then — no JSON, no field names, no HTTP codes.
  #    Best for: smoke tests, minimal-confidence checks.

  @smoke
  Scenario: US active basic-card customer is eligible for Plan It Amount
    Given the following mock interactions are registered:
      | interaction                              |
      | read-member/us-active-basic              |
      | read-lending-config/us-plan-it-enabled   |
    And a "us" market customer
    When the customer checks eligibility for feature "PLAN-IT-AMOUNT"
    Then the customer should be eligible

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
  #    Feature file says WHAT to validate; a JSON expectation file under
  #    src/test/resources/expectations/ defines the structure and matchers.
  #    Best for: responses with many fields, nested objects, or arrays.

  Scenario: Eligible response contains full lending configuration
    Given the following mock interactions are registered:
      | interaction                              |
      | read-member/us-active-basic              |
      | read-lending-config/us-plan-it-enabled   |
    And a "us" market customer
    When the customer checks eligibility for feature "PLAN-IT-AMOUNT"
    Then the customer should be eligible
    And the response body should match expectation "read-lending-plan-eligibility/us-eligible-plan-it-amount"

  # ── Template 4: Chained API calls ─────────────────────────────────────
  #    Call API A, then call API B, assert business consistency across both.
  #    All HTTP details, endpoints, JSON payloads live in step definitions.
  #    Best for: workflows where APIs feed into each other.

  Scenario: Eligibility installment limit matches lending configuration
    Given the following mock interactions are registered:
      | interaction                              |
      | read-member/us-active-basic              |
      | read-lending-config/us-plan-it-enabled   |
    And a "us" market customer
    When the customer checks eligibility for feature "PLAN-IT-AMOUNT"
    Then the customer should be eligible
    When the lending configuration is retrieved for market "US"
    Then the eligibility and lending configuration should agree on installment limits
