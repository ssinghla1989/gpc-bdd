@regression @lending-plan-eligibility
Feature: ReadLendingPlanEligibility API
  As a lending service consumer
  I want to check customer eligibility for Plan It features
  So that I can offer the correct lending products based on market rules

  # ── Negative: market-level ineligibility ──────────────────────────────

  Scenario: Japan market customer is ineligible for Plan It Amount
    Given the following mock interactions are registered:
      | interaction                              |
      | read-member/japan-market-response        |
    And a "japan" market customer
    When the customer checks eligibility for feature "PLAN-IT-AMOUNT"
    Then the request should be declined with status 422 and error "Feature name is invalid for japan market"

  # ── Negative: japan market with supplementary card ────────────────────

  Scenario: Japan market customer with supplementary card is ineligible
    Given the following mock interactions are registered:
      | interaction                              |
      | read-member/japan-active-supplementary   |
    And a "japan" market customer
    When the customer checks eligibility for feature "PLAN-IT-AMOUNT"
    Then the request should be declined with status 422 and error "Feature name is invalid for japan market"

  # ── Positive: fully eligible customer ─────────────────────────────────

  @smoke
  Scenario: US market active account with basic card is eligible for Plan It Amount
    Given the following mock interactions are registered:
      | interaction                              |
      | read-member/us-active-basic              |
      | read-lending-config/us-plan-it-enabled   |
    And a "us" market customer
    When the customer checks eligibility for feature "PLAN-IT-AMOUNT"
    Then the response status code should be 200
    And the response field "eligible" should be "true"
    And the response should contain field "maxInstallments"

  # ── Negative: inactive account ────────────────────────────────────────

  Scenario: Inactive account is declined regardless of market or card
    Given the following mock interactions are registered:
      | interaction                              |
      | read-member/us-inactive-basic            |
    And a "us" market customer
    When the customer checks eligibility for feature "PLAN-IT-AMOUNT"
    Then the request should be declined with status 422 and error "Account is not active"

  # ── Negative: closed account ──────────────────────────────────────────

  Scenario: Closed account is declined for Plan It
    Given the following mock interactions are registered:
      | interaction                              |
      | read-member/us-closed-basic              |
    And a "us" market customer
    When the customer checks eligibility for feature "PLAN-IT-TRANSACTION"
    Then the request should be declined with status 422 and error "Account is not active"

  # ── Negative: supplementary card ──────────────────────────────────────

  Scenario: Supplementary card holder is ineligible for Plan It
    Given the following mock interactions are registered:
      | interaction                              |
      | read-member/us-active-supplementary      |
      | read-lending-config/us-plan-it-enabled   |
    And a "us" market customer
    When the customer checks eligibility for feature "PLAN-IT-AMOUNT"
    Then the request should be declined with status 422 and error "Supplementary card holders are not eligible"

  # ── Negative: invalid feature name ────────────────────────────────────

  Scenario: Requesting an unknown feature name returns validation error
    Given the following mock interactions are registered:
      | interaction                              |
      | read-member/us-active-basic              |
    And a "us" market customer
    When the customer checks eligibility for feature "INVALID-FEATURE"
    Then the request should be declined with status 422 and error "Feature name is invalid"

  # ── Positive: UK market eligible ──────────────────────────────────────

  Scenario: UK market active account with basic card is eligible
    Given the following mock interactions are registered:
      | interaction                              |
      | read-member/uk-active-basic              |
    And a "uk" market customer
    When the customer checks eligibility for feature "PLAN-IT-AMOUNT"
    Then the response status code should be 200
    And the response field "eligible" should be "true"
