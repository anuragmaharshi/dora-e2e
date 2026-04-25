package com.dora.e2e.support;

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

    // ---- accessors ----

    public String getJwtToken() { return jwtToken; }
    public void setJwtToken(String jwtToken) { this.jwtToken = jwtToken; }

    public Response getLastResponse() { return lastResponse; }
    public void setLastResponse(Response lastResponse) { this.lastResponse = lastResponse; }

    public WebDriver getDriver() { return driver; }
    public void setDriver(WebDriver driver) { this.driver = driver; }
}
