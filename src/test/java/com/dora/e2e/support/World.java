package com.dora.e2e.support;

import com.dora.e2e.clients.AuditDbClient.TamperResult;
import io.restassured.response.Response;
import org.openqa.selenium.WebDriver;

/**
 * Per-scenario shared state bag injected via PicoContainer.
 * Each scenario gets a fresh instance — no shared mutable static state.
 */
public class World {

    // ---- API state ----

    /** The JWT token obtained during the current scenario. */
    private String jwtToken;

    /** The last RestAssured response received. */
    private Response lastResponse;

    // ---- UI state ----

    /** The WebDriver instance for this scenario (null for API-only scenarios). */
    private WebDriver driver;

    // ---- LLD-03 audit smoke state ----

    /**
     * UUID string generated fresh per scenario for the probe entity.
     * Ensures parallel scenario runs do not share audit rows.
     */
    private String probeEntityId;

    /**
     * Result of the direct-JDBC tamper attempt (LLD-03 immutability scenario only).
     * Null for all other scenarios.
     */
    private TamperResult lastTamperResult;

    // ---- accessors ----

    public String getJwtToken() { return jwtToken; }
    public void setJwtToken(String jwtToken) { this.jwtToken = jwtToken; }

    public Response getLastResponse() { return lastResponse; }
    public void setLastResponse(Response lastResponse) { this.lastResponse = lastResponse; }

    public WebDriver getDriver() { return driver; }
    public void setDriver(WebDriver driver) { this.driver = driver; }

    public String getProbeEntityId() { return probeEntityId; }
    public void setProbeEntityId(String probeEntityId) { this.probeEntityId = probeEntityId; }

    public TamperResult getLastTamperResult() { return lastTamperResult; }
    public void setLastTamperResult(TamperResult lastTamperResult) { this.lastTamperResult = lastTamperResult; }
}
