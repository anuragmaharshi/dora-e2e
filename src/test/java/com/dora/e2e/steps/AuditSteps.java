package com.dora.e2e.steps;

import com.dora.e2e.clients.AuditApiClient;
import com.dora.e2e.clients.AuditDbClient;
import com.dora.e2e.clients.AuditDbClient.DbUnreachableException;
import com.dora.e2e.clients.AuditDbClient.TamperResult;
import com.dora.e2e.clients.AuthApiClient;
import com.dora.e2e.support.World;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for LLD-03 audit trail smoke scenarios.
 *
 * <p>All API calls go through {@link AuditApiClient} (RestAssured).
 * Direct-DB calls for the immutability scenario go through {@link AuditDbClient} (JDBC).
 * Authentication reuses {@link AuthApiClient} from the existing scaffold.
 *
 * <p>Steps are injected with {@link World} via PicoContainer — one World per scenario,
 * so parallel execution is safe.
 */
public class AuditSteps {

    private final World world;
    private final AuditApiClient auditClient;
    private final AuthApiClient authClient;
    private final AuditDbClient dbClient;

    public AuditSteps(World world) {
        this.world       = world;
        this.auditClient = new AuditApiClient();
        this.authClient  = new AuthApiClient();
        this.dbClient    = new AuditDbClient();
    }

    // -------------------------------------------------------------------------
    // Background steps
    // -------------------------------------------------------------------------

    /**
     * "Given the DORA stack is healthy" is satisfied by the {@code @BeforeAll} gate
     * in {@link com.dora.e2e.support.Hooks}.  This step exists for Gherkin readability only.
     */
    @Given("the DORA stack is healthy")
    public void theDoraStackIsHealthy() {
        // No-op: Hooks.@BeforeAll already verified /actuator/health.
        // If the stack is unhealthy, the suite has already aborted before reaching here.
    }

    /**
     * Generates a unique UUID for the probe entity so that parallel scenario runs
     * do not collide on the same audit rows.
     */
    @And("a unique probe entity id is generated for this scenario")
    public void aUniqueProbeEntityIdIsGeneratedForThisScenario() {
        world.setProbeEntityId(UUID.randomUUID().toString());
    }

    // -------------------------------------------------------------------------
    // Seed step — POST /api/v1/_test/audit-emit
    // -------------------------------------------------------------------------

    /**
     * Seeds a probe audit row via the test-profile endpoint.
     * If the endpoint returns 404 the scenario is marked {@code @Pending} — the test-profile
     * endpoint is only active when Spring profile {@code test} is enabled.
     */
    @When("the test audit-emit endpoint is called with entityType {string} and action {string}")
    public void theTestAuditEmitEndpointIsCalledWith(String entityType, String action) {
        String entityId = world.getProbeEntityId();
        assertThat(entityId)
                .as("Probe entity id must be set before calling audit-emit")
                .isNotBlank();

        Response response = auditClient.emitAuditRow(entityType, entityId, action);

        if (response.statusCode() == 404) {
            throw new PendingException(
                    "POST /api/v1/_test/audit-emit returned 404 — the test-profile endpoint is " +
                    "not active in this environment (Spring profile 'test' not enabled). " +
                    "Marking scenario as @Pending.");
        }

        world.setLastResponse(response);
    }

    /**
     * Ensures the probe audit row exists before scenarios that depend on it.
     * Seeds the row by calling the test-emit endpoint; pends if the endpoint is absent.
     */
    @Given("the test audit row for the probe entity exists")
    public void theTestAuditRowForTheProbeEntityExists() {
        String entityId = world.getProbeEntityId();
        assertThat(entityId)
                .as("Probe entity id must be set in the Background step")
                .isNotBlank();

        Response emitResponse = auditClient.emitAuditRow("PROBE", entityId, "SYSTEM");

        if (emitResponse.statusCode() == 404) {
            throw new PendingException(
                    "POST /api/v1/_test/audit-emit returned 404 — the test-profile endpoint is " +
                    "not active in this environment. Marking scenario as @Pending.");
        }

        assertThat(emitResponse.statusCode())
                .as("Seeding audit row via /api/v1/_test/audit-emit must return 2xx. " +
                    "Body: %s", emitResponse.body().asString())
                .isBetween(200, 299);
    }

    // -------------------------------------------------------------------------
    // Auth step (reuses AuthApiClient from existing scaffold)
    // -------------------------------------------------------------------------

    @Given("the user {string} has logged in with password {string}")
    public void theUserHasLoggedInWithPassword(String email, String password) {
        Response loginResponse = authClient.login(email, password);
        assertThat(loginResponse.statusCode())
                .as("Login for user %s must succeed (expected 200). Body: %s",
                        email, loginResponse.body().asString())
                .isEqualTo(200);

        String token = loginResponse.jsonPath().getString("token");
        assertThat(token)
                .as("JWT token must be present in the login response for user %s", email)
                .isNotBlank();

        world.setJwtToken(token);
    }

    // -------------------------------------------------------------------------
    // API query step — GET /api/v1/audit
    // -------------------------------------------------------------------------

    @When("^they call GET /api/v1/audit with entity \"([^\"]*)\" and the probe entity id$")
    public void theyCallGetAuditWithEntityAndProbeId(String entityType) {
        String jwt      = world.getJwtToken();
        String entityId = world.getProbeEntityId();

        assertThat(jwt)
                .as("JWT must be set before calling the audit endpoint")
                .isNotBlank();
        assertThat(entityId)
                .as("Probe entity id must be set before calling the audit endpoint")
                .isNotBlank();

        Response response = auditClient.queryAudit(jwt, entityType, entityId);
        world.setLastResponse(response);
    }

    // -------------------------------------------------------------------------
    // Assertion steps
    // -------------------------------------------------------------------------

    // Note: "the response status is {int}" is defined in ApiAuthSteps and is reused here —
    // Cucumber resolves it from the shared glue path (com.dora.e2e.steps).

    /** Allows "200 or 201" wording used in the seed scenario. */
    @Then("the response status is one of 200 or 201")
    public void theResponseStatusIsOneOf200Or201() {
        int actual = world.getLastResponse().statusCode();
        assertThat(actual)
                .as("Expected HTTP 200 or 201 but got %d. Body: %s",
                        actual, world.getLastResponse().body().asString())
                .isIn(200, 201);
    }

    @Then("the response content array is not empty")
    public void theResponseContentArrayIsNotEmpty() {
        List<?> content = world.getLastResponse().jsonPath().getList("content");
        assertThat(content)
                .as("Response 'content' array must be present and non-empty. Body: %s",
                        world.getLastResponse().body().asString())
                .isNotNull()
                .isNotEmpty();
    }

    @Then("the first audit entry has non-null fields: entityType, entityId, action, actorUsername, createdAt")
    public void theFirstAuditEntryHasNonNullRequiredFields() {
        // Extract the first element of the content array
        String entityType  = world.getLastResponse().jsonPath().getString("content[0].entityType");
        String entityId    = world.getLastResponse().jsonPath().getString("content[0].entityId");
        String action      = world.getLastResponse().jsonPath().getString("content[0].action");
        String actorUser   = world.getLastResponse().jsonPath().getString("content[0].actorUsername");
        String createdAt   = world.getLastResponse().jsonPath().getString("content[0].createdAt");

        assertThat(entityType)
                .as("content[0].entityType must not be null or blank")
                .isNotBlank();
        assertThat(entityId)
                .as("content[0].entityId must not be null or blank")
                .isNotBlank();
        assertThat(action)
                .as("content[0].action must not be null or blank")
                .isNotBlank();
        assertThat(actorUser)
                .as("content[0].actorUsername must not be null or blank")
                .isNotBlank();
        assertThat(createdAt)
                .as("content[0].createdAt must not be null or blank")
                .isNotBlank();
    }

    // -------------------------------------------------------------------------
    // Immutability step — direct JDBC
    // -------------------------------------------------------------------------

    @When("a direct SQL UPDATE attempts to change the action to {string} for the probe entity")
    public void aDirectSqlUpdateAttemptsToTamper(String tamperedAction) {
        // The tampered action value is captured from the step but the SQL is fixed — we
        // always try to SET action='TAMPERED' to exercise the trigger regardless of the
        // parameter value supplied in the feature file.
        String entityId = world.getProbeEntityId();
        assertThat(entityId)
                .as("Probe entity id must be set before attempting the immutability check")
                .isNotBlank();

        try {
            TamperResult result = dbClient.tryTamperByEntityId(entityId);
            // Store the result in a way the next step can assert on it.
            // We abuse lastResponse's absence here — we piggyback on a World string field.
            // The Then step reads the TamperResult from the thread-local we set below.
            world.setLastTamperResult(result);
        } catch (DbUnreachableException ex) {
            throw new PendingException(
                    "Database is not reachable at " + System.getProperty("db.url", "jdbc:postgresql://localhost:5432/dora") +
                    ". Cannot verify immutability trigger. Marking scenario as @Pending. " +
                    "Root cause: " + ex.getMessage());
        }
    }

    @Then("the UPDATE is rejected or affects zero rows")
    public void theUpdateIsRejectedOrAffectsZeroRows() {
        TamperResult result = world.getLastTamperResult();
        assertThat(result)
                .as("Expected the DB trigger to reject the UPDATE (TRIGGER_REJECTED) or to find " +
                    "no matching row (ZERO_ROWS_AFFECTED), but the UPDATE modified rows — " +
                    "the audit_log immutability trigger is NOT working correctly.")
                .isIn(TamperResult.TRIGGER_REJECTED, TamperResult.ZERO_ROWS_AFFECTED);
    }
}
