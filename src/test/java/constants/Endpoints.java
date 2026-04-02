package constants;

/**
 * Central registry of API endpoint paths.
 * Add new endpoints here as you onboard APIs.
 */
public final class Endpoints {

    private Endpoints() {
    }

    // Mock server management
    public static final String MOCK_INTERACTIONS = "/api/interactions";

    // Lending
    public static final String READ_LENDING_PLAN_ELIGIBILITY = "/ReadLendingPlanEligibility.v2";
    public static final String READ_LENDING_CONFIG = "/ReadLendingConfig.v1";
}
