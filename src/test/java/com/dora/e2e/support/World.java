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

    // ---- LLD-04 admin smoke state ----

    /**
     * JWT token for the bank-user role (ops@dora.local).
     * Kept separate from jwtToken so admin and bank-user steps can coexist in one scenario.
     */
    private String bankUserJwtToken;

    /**
     * ID of a critical service created or looked up during the current scenario.
     * Used to chain create → archive steps without coupling step implementations.
     */
    private String lastCriticalServiceId;

    /**
     * Name (with UUID suffix) of the last critical service created in the current scenario.
     * Used to assert the created service name when a UUID suffix was appended for uniqueness.
     */
    private String lastCriticalServiceName;

    // ---- LLD-05 incident state ----

    /**
     * UUID of the last incident created during the current scenario.
     * Used to chain create → attach / link asset steps.
     */
    private String lastIncidentId;

    /**
     * JWT token for the incident manager role (incident@dora.local).
     * Kept separate from jwtToken so ops and manager steps can coexist.
     */
    private String incidentManagerJwtToken;

    /**
     * UUID of the last attachment created during the current scenario.
     * Used to chain presigned-URL → upload → complete steps.
     */
    private String lastAttachmentId;

    /**
     * The presigned upload URL returned by the attachments endpoint.
     * Stored here so the upload step can retrieve it without re-calling the API.
     */
    private String lastPresignedUploadUrl;

    /**
     * JWT token for platform admin (platform@dora.local).
     * Used by AC-8 platform admin block scenarios.
     */
    private String platformAdminJwtToken;

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

    public String getBankUserJwtToken() { return bankUserJwtToken; }
    public void setBankUserJwtToken(String bankUserJwtToken) { this.bankUserJwtToken = bankUserJwtToken; }

    public String getLastCriticalServiceId() { return lastCriticalServiceId; }
    public void setLastCriticalServiceId(String lastCriticalServiceId) { this.lastCriticalServiceId = lastCriticalServiceId; }

    public String getLastCriticalServiceName() { return lastCriticalServiceName; }
    public void setLastCriticalServiceName(String lastCriticalServiceName) { this.lastCriticalServiceName = lastCriticalServiceName; }

    public String getLastIncidentId() { return lastIncidentId; }
    public void setLastIncidentId(String lastIncidentId) { this.lastIncidentId = lastIncidentId; }

    public String getIncidentManagerJwtToken() { return incidentManagerJwtToken; }
    public void setIncidentManagerJwtToken(String incidentManagerJwtToken) { this.incidentManagerJwtToken = incidentManagerJwtToken; }

    public String getLastAttachmentId() { return lastAttachmentId; }
    public void setLastAttachmentId(String lastAttachmentId) { this.lastAttachmentId = lastAttachmentId; }

    public String getLastPresignedUploadUrl() { return lastPresignedUploadUrl; }
    public void setLastPresignedUploadUrl(String lastPresignedUploadUrl) { this.lastPresignedUploadUrl = lastPresignedUploadUrl; }

    public String getPlatformAdminJwtToken() { return platformAdminJwtToken; }
    public void setPlatformAdminJwtToken(String platformAdminJwtToken) { this.platformAdminJwtToken = platformAdminJwtToken; }
}
