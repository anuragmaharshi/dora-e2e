package com.dora.e2e.support;

/**
 * Central configuration — reads from system properties or environment variables,
 * falls back to the default local-stack values defined in the assignment brief.
 */
public final class Config {

    private Config() {}

    public static final String API_BASE_URL =
            System.getProperty("dora.api.url",
                    System.getenv().getOrDefault("DORA_API_URL", "http://localhost:8080"));

    public static final String FRONTEND_BASE_URL =
            System.getProperty("dora.frontend.url",
                    System.getenv().getOrDefault("DORA_FRONTEND_URL", "http://localhost:4200"));

    public static final String HEALTH_ENDPOINT = API_BASE_URL + "/actuator/health";

    /** Whether to run the browser in headless mode (default: true). */
    public static final boolean HEADLESS =
            Boolean.parseBoolean(
                    System.getProperty("dora.headless",
                            System.getenv().getOrDefault("DORA_HEADLESS", "true")));

    /** Explicit wait timeout in seconds. */
    public static final int WAIT_TIMEOUT_SECONDS =
            Integer.parseInt(
                    System.getProperty("dora.wait.timeout",
                            System.getenv().getOrDefault("DORA_WAIT_TIMEOUT", "10")));

    // --------------- seeded test users ---------------

    public static final String DEFAULT_PASSWORD = "ChangeMe!23";

    public static final String USER_PLATFORM_ADMIN  = "platform@dora.local";
    public static final String USER_OPS_ANALYST     = "ops@dora.local";
    public static final String USER_INCIDENT_MANAGER = "incident@dora.local";
    public static final String USER_COMPLIANCE      = "compliance@dora.local";
    public static final String USER_CISO            = "ciso@dora.local";
    public static final String USER_BOARD_VIEWER    = "board@dora.local";
    public static final String USER_SYSTEM          = "system@dora.local";
}
