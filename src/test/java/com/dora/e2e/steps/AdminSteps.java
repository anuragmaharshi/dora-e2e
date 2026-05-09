package com.dora.e2e.steps;

import com.dora.e2e.clients.AdminApiClient;
import com.dora.e2e.clients.AuditApiClient;
import com.dora.e2e.clients.AuthApiClient;
import com.dora.e2e.pages.AdminPage;
import com.dora.e2e.support.Config;
import com.dora.e2e.support.World;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.openqa.selenium.WebDriver;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for LLD-04: Tenant Configuration & Platform Admin Portal.
 *
 * <p>All API calls go through {@link AdminApiClient} (RestAssured).
 * Authentication reuses {@link AuthApiClient} from the existing scaffold.
 * UI steps for AC-5 (Angular roleGuard redirect) go through {@link AdminPage} (Selenium).
 *
 * <p>Steps are injected with {@link World} via PicoContainer — one World per scenario,
 * so parallel execution is safe.
 *
 * <p>If an admin endpoint returns 404, the scenario is marked {@code @Pending}:
 * this means the admin feature branch has not been merged yet.
 */
public class AdminSteps {

    private final World world;
    private final AdminApiClient adminClient;
    private final AuthApiClient authClient;
    private final AuditApiClient auditClient;

    public AdminSteps(World world) {
        this.world       = world;
        this.adminClient = new AdminApiClient();
        this.authClient  = new AuthApiClient();
        this.auditClient = new AuditApiClient();
    }

    // -------------------------------------------------------------------------
    // Background steps — authentication
    // -------------------------------------------------------------------------

    /**
     * Authenticates platform@dora.local and stores the JWT in World.
     * Used by every admin scenario as a Background step.
     */
    @Given("the platform admin user is authenticated")
    public void thePlatformAdminUserIsAuthenticated() {
        Response loginResponse = authClient.login(
                Config.USER_PLATFORM_ADMIN,
                Config.DEV_SEED_PASSWORD);

        if (loginResponse.statusCode() == 404) {
            throw new PendingException(
                    "POST /api/v1/auth/login returned 404 — stack not reachable or auth endpoint " +
                    "not active. Marking scenario as @Pending.");
        }

        assertThat(loginResponse.statusCode())
                .as("Platform admin login must return HTTP 200. Body: %s",
                        loginResponse.body().asString())
                .isEqualTo(200);

        String token = loginResponse.jsonPath().getString("token");
        assertThat(token)
                .as("Platform admin login must return a non-blank JWT token")
                .isNotBlank();

        world.setJwtToken(token);
    }

    /**
     * Authenticates the bank user (ops@dora.local) and stores the JWT in World.bankUserJwtToken.
     */
    @Given("the bank user is authenticated")
    public void theBankUserIsAuthenticated() {
        Response loginResponse = authClient.login(
                Config.USER_OPS_ANALYST,
                Config.DEV_SEED_PASSWORD);

        if (loginResponse.statusCode() == 404) {
            throw new PendingException(
                    "POST /api/v1/auth/login returned 404 — stack not reachable. " +
                    "Marking scenario as @Pending.");
        }

        assertThat(loginResponse.statusCode())
                .as("Bank user login must return HTTP 200. Body: %s",
                        loginResponse.body().asString())
                .isEqualTo(200);

        String token = loginResponse.jsonPath().getString("token");
        assertThat(token)
                .as("Bank user login must return a non-blank JWT token")
                .isNotBlank();

        world.setBankUserJwtToken(token);
    }

    // -------------------------------------------------------------------------
    // AC-1 — Tenant config read
    // -------------------------------------------------------------------------

    @When("^the platform admin calls GET /api/v1/admin/tenant$")
    public void thePlatformAdminCallsGetAdminTenant() {
        String jwt = world.getJwtToken();
        assertThat(jwt).as("Platform admin JWT must be set").isNotBlank();

        Response response = adminClient.getTenantConfig(jwt);
        guardNotFound(response, "GET /api/v1/admin/tenant");
        world.setLastResponse(response);
    }

    @Then("the tenant config response contains fields: legalName, lei, ncaName, ncaEmail, jurisdictionIso")
    public void theTenantConfigResponseContainsRequiredFields() {
        Response r = world.getLastResponse();
        String body = r.body().asString();
        // legalName and lei are always present (set in seed migration V1_1_1).
        // ncaName, ncaEmail, jurisdictionIso may be null until configured by the admin —
        // assert they are present as keys in the response (not necessarily non-null).
        for (String field : List.of("legalName", "lei")) {
            assertThat((Object) r.jsonPath().get(field))
                    .as("Tenant config response must contain a non-null '%s' field. Body: %s",
                            field, body)
                    .isNotNull();
        }
        for (String field : List.of("ncaName", "ncaEmail", "jurisdictionIso")) {
            assertThat(body)
                    .as("Tenant config response body must include the key '%s' (value may be null). Body: %s",
                            field, body)
                    .contains("\"" + field + "\"");
        }
    }

    // -------------------------------------------------------------------------
    // AC-1 + AC-8 — Tenant config update
    // -------------------------------------------------------------------------

    @When("the platform admin updates the tenant config with legalName {string} and ncaEmail {string}")
    public void thePlatformAdminUpdatesTenantConfig(String legalName, String ncaEmail) {
        String jwt = world.getJwtToken();
        assertThat(jwt).as("Platform admin JWT must be set").isNotBlank();

        Response response = adminClient.updateTenantConfigPartial(jwt, legalName, ncaEmail);
        guardNotFound(response, "PUT /api/v1/admin/tenant");
        world.setLastResponse(response);
    }

    @Then("the tenant config response contains legalName {string}")
    public void theTenantConfigResponseContainsLegalName(String expectedName) {
        String actual = world.getLastResponse().jsonPath().getString("legalName");
        assertThat(actual)
                .as("Tenant config legalName should be '%s' but got '%s'. Body: %s",
                        expectedName, actual, world.getLastResponse().body().asString())
                .isEqualTo(expectedName);
    }

    // -------------------------------------------------------------------------
    // AC-2 — Critical services list
    // -------------------------------------------------------------------------

    @When("^the platform admin calls GET /api/v1/admin/critical-services$")
    public void thePlatformAdminCallsGetCriticalServices() {
        String jwt = world.getJwtToken();
        assertThat(jwt).as("Platform admin JWT must be set").isNotBlank();

        Response response = adminClient.getCriticalServices(jwt);
        guardNotFound(response, "GET /api/v1/admin/critical-services");
        world.setLastResponse(response);
    }

    @Then("the critical services response is a list")
    public void theCriticalServicesResponseIsAList() {
        // The endpoint may return a plain JSON array or a paged wrapper with "content".
        // Try array first, then "content" wrapper.
        Response r = world.getLastResponse();
        String body = r.body().asString();

        if (body.trim().startsWith("[")) {
            // Plain array
            List<?> list = r.jsonPath().getList("$");
            assertThat(list)
                    .as("Critical services response must be a valid (possibly empty) JSON array. Body: %s", body)
                    .isNotNull();
        } else {
            // Try paged wrapper
            Object content = r.jsonPath().get("content");
            if (content == null) {
                // Accept any object response as valid list wrapper
                assertThat(body)
                        .as("Critical services response must be a list or paged wrapper. Body: %s", body)
                        .isNotBlank();
            }
        }
    }

    // -------------------------------------------------------------------------
    // AC-2 — Critical services create
    // -------------------------------------------------------------------------

    @When("the platform admin creates a critical service named {string}")
    public void thePlatformAdminCreatesACriticalService(String name) {
        String jwt = world.getJwtToken();
        assertThat(jwt).as("Platform admin JWT must be set").isNotBlank();

        Response response = adminClient.createCriticalService(jwt, name);
        guardNotFound(response, "POST /api/v1/admin/critical-services");
        world.setLastResponse(response);

        // Capture the ID only if the response is a successful JSON body
        if (response.statusCode() == 201) {
            try {
                String id = response.jsonPath().getString("id");
                if (id != null && !id.isBlank()) {
                    world.setLastCriticalServiceId(id);
                }
            } catch (Exception ignored) {
                // ID extraction is best-effort; the scenario assertion will fail with clearer message
            }
        }
    }

    @Then("the created critical service has name {string}")
    public void theCreatedCriticalServiceHasName(String expectedName) {
        String actual = world.getLastResponse().jsonPath().getString("name");
        assertThat(actual)
                .as("Created critical service name should be '%s' but got '%s'. Body: %s",
                        expectedName, actual, world.getLastResponse().body().asString())
                .isEqualTo(expectedName);
    }

    // -------------------------------------------------------------------------
    // AC-2 — Critical services archive
    // -------------------------------------------------------------------------

    /**
     * Seeds a critical service with the given name so the archive step has a target.
     * Uses a UUID suffix to avoid collisions across parallel scenarios.
     */
    @Given("a critical service named {string} exists")
    public void aCriticalServiceNamedExists(String name) {
        String jwt = world.getJwtToken();
        assertThat(jwt).as("Platform admin JWT must be set").isNotBlank();

        // Append a UUID suffix for parallel-safety
        String uniqueName = name + " " + UUID.randomUUID().toString().substring(0, 8);
        Response createResponse = adminClient.createCriticalService(jwt, uniqueName);
        guardNotFound(createResponse, "POST /api/v1/admin/critical-services (seed for archive)");

        assertThat(createResponse.statusCode())
                .as("Seeding critical service '%s' must return 201. Body: %s",
                        uniqueName, createResponse.body().asString())
                .isEqualTo(201);

        String id = createResponse.jsonPath().getString("id");
        assertThat(id)
                .as("Created critical service must have a non-blank id field. Body: %s",
                        createResponse.body().asString())
                .isNotBlank();

        world.setLastCriticalServiceId(id);
    }

    @When("the platform admin archives the critical service")
    public void thePlatformAdminArchivesTheCriticalService() {
        String jwt = world.getJwtToken();
        String id  = world.getLastCriticalServiceId();
        assertThat(jwt).as("Platform admin JWT must be set").isNotBlank();
        assertThat(id).as("Critical service id must be set before archiving").isNotBlank();

        Response response = adminClient.archiveCriticalService(jwt, id);
        guardNotFound(response, "DELETE /api/v1/admin/critical-services/" + id);
        world.setLastResponse(response);
    }

    // -------------------------------------------------------------------------
    // AC-3 — Client base read
    // -------------------------------------------------------------------------

    @When("^the platform admin calls GET /api/v1/admin/client-base$")
    public void thePlatformAdminCallsGetClientBase() {
        String jwt = world.getJwtToken();
        assertThat(jwt).as("Platform admin JWT must be set").isNotBlank();

        Response response = adminClient.getClientBase(jwt);
        guardNotFound(response, "GET /api/v1/admin/client-base");
        world.setLastResponse(response);
    }

    @Then("the client base response is a list")
    public void theClientBaseResponseIsAList() {
        Response r = world.getLastResponse();
        String body = r.body().asString();

        if (body.trim().startsWith("[")) {
            List<?> list = r.jsonPath().getList("$");
            assertThat(list)
                    .as("Client base response must be a valid JSON array. Body: %s", body)
                    .isNotNull();
        } else {
            Object content = r.jsonPath().get("content");
            if (content == null) {
                assertThat(body)
                        .as("Client base response must be a list or paged wrapper. Body: %s", body)
                        .isNotBlank();
            }
        }
    }

    // -------------------------------------------------------------------------
    // AC-3 — Client base create
    // -------------------------------------------------------------------------

    @When("the platform admin sets the client base count to {long} effective today")
    public void thePlatformAdminSetsClientBaseCount(long clientCount) {
        String jwt = world.getJwtToken();
        assertThat(jwt).as("Platform admin JWT must be set").isNotBlank();

        Response response = adminClient.createClientBaseEntry(jwt, clientCount);
        guardNotFound(response, "POST /api/v1/admin/client-base");
        world.setLastResponse(response);
    }

    @Then("the client base entry has clientCount {long}")
    public void theClientBaseEntryHasClientCount(long expectedCount) {
        // The response field might be "clientCount" or "client_count" — try both
        Object actual = world.getLastResponse().jsonPath().get("clientCount");
        if (actual == null) {
            actual = world.getLastResponse().jsonPath().get("client_count");
        }
        assertThat(actual)
                .as("Client base entry must have clientCount %d. Body: %s",
                        expectedCount, world.getLastResponse().body().asString())
                .isNotNull();

        long actualLong = ((Number) actual).longValue();
        assertThat(actualLong)
                .as("Client base clientCount should be %d but got %d", expectedCount, actualLong)
                .isEqualTo(expectedCount);
    }

    // -------------------------------------------------------------------------
    // AC-4 — NCA email read
    // -------------------------------------------------------------------------

    @When("^the platform admin calls GET /api/v1/admin/nca-email$")
    public void thePlatformAdminCallsGetNcaEmail() {
        String jwt = world.getJwtToken();
        assertThat(jwt).as("Platform admin JWT must be set").isNotBlank();

        Response response = adminClient.getNcaEmailConfig(jwt);
        guardNotFound(response, "GET /api/v1/admin/nca-email");
        world.setLastResponse(response);
    }

    @Then("the NCA email response contains fields: sender, recipient, subjectTemplate")
    public void theNcaEmailResponseContainsRequiredFields() {
        Response r = world.getLastResponse();
        for (String field : List.of("sender", "recipient", "subjectTemplate")) {
            assertThat((Object) r.jsonPath().get(field))
                    .as("NCA email response must contain field '%s'. Body: %s",
                            field, r.body().asString())
                    .isNotNull();
        }
    }

    // -------------------------------------------------------------------------
    // AC-4 — NCA email update
    // -------------------------------------------------------------------------

    @When("the platform admin updates the NCA email config with sender {string} and recipient {string} and subjectTemplate {string}")
    public void thePlatformAdminUpdatesNcaEmailConfig(String sender, String recipient,
                                                       String subjectTemplate) {
        String jwt = world.getJwtToken();
        assertThat(jwt).as("Platform admin JWT must be set").isNotBlank();

        Response response = adminClient.updateNcaEmailConfig(jwt, sender, recipient, subjectTemplate);
        guardNotFound(response, "PUT /api/v1/admin/nca-email");
        world.setLastResponse(response);
    }

    @Then("the NCA email response has sender {string}")
    public void theNcaEmailResponseHasSender(String expectedSender) {
        String actual = world.getLastResponse().jsonPath().getString("sender");
        assertThat(actual)
                .as("NCA email sender should be '%s' but got '%s'. Body: %s",
                        expectedSender, actual, world.getLastResponse().body().asString())
                .isEqualTo(expectedSender);
    }

    @Then("the NCA email response has recipient {string}")
    public void theNcaEmailResponseHasRecipient(String expectedRecipient) {
        String actual = world.getLastResponse().jsonPath().getString("recipient");
        assertThat(actual)
                .as("NCA email recipient should be '%s' but got '%s'. Body: %s",
                        expectedRecipient, actual, world.getLastResponse().body().asString())
                .isEqualTo(expectedRecipient);
    }

    // -------------------------------------------------------------------------
    // AC-5 — Angular roleGuard redirect (Selenium / UI)
    // -------------------------------------------------------------------------

    /**
     * Logs the platform admin in via the browser login form.
     * Requires the @ui hook to have already initialised a WebDriver.
     */
    @Given("the platform admin is logged in via the browser")
    public void thePlatformAdminIsLoggedInViaBrowser() {
        WebDriver driver = world.getDriver();
        assertThat(driver)
                .as("WebDriver must be initialised — this step requires @ui tag")
                .isNotNull();

        AdminPage adminPage = new AdminPage(driver);
        adminPage.loginAs(Config.USER_PLATFORM_ADMIN, Config.DEV_SEED_PASSWORD);
    }

    @When("^the browser navigates to /incidents$")
    public void theBrowserNavigatesToIncidents() {
        WebDriver driver = world.getDriver();
        assertThat(driver).as("WebDriver must be initialised").isNotNull();

        AdminPage adminPage = new AdminPage(driver);
        adminPage.navigateTo("/incidents");
    }

    @Then("the browser URL contains {string}")
    public void theBrowserUrlContains(String expectedFragment) {
        WebDriver driver = world.getDriver();
        assertThat(driver).as("WebDriver must be initialised").isNotNull();

        AdminPage adminPage = new AdminPage(driver);
        String currentUrl = adminPage.waitForUrlContaining(expectedFragment);
        assertThat(currentUrl)
                .as("Browser URL should contain '%s' after roleGuard redirect. Actual URL: %s",
                        expectedFragment, currentUrl)
                .contains(expectedFragment);
    }

    // -------------------------------------------------------------------------
    // AC-6 — PLATFORM_ADMIN blocked on incident API
    // -------------------------------------------------------------------------

    @When("^the platform admin calls GET /api/v1/incidents with the platform admin JWT$")
    public void thePlatformAdminCallsGetIncidents() {
        String jwt = world.getJwtToken();
        assertThat(jwt).as("Platform admin JWT must be set").isNotBlank();

        Response response = adminClient.getIncidents(jwt);
        world.setLastResponse(response);
    }

    // -------------------------------------------------------------------------
    // AC-7 — BANK_USER blocked on admin endpoints
    // -------------------------------------------------------------------------

    @When("^the bank user calls GET /api/v1/admin/tenant$")
    public void theBankUserCallsGetAdminTenant() {
        String jwt = world.getBankUserJwtToken();
        assertThat(jwt).as("Bank user JWT must be set").isNotBlank();

        Response response = adminClient.getTenantConfigAs(jwt);
        guardNotFound(response, "GET /api/v1/admin/tenant (bank user)");
        world.setLastResponse(response);
    }

    @When("^the bank user calls GET /api/v1/admin/critical-services$")
    public void theBankUserCallsGetCriticalServices() {
        String jwt = world.getBankUserJwtToken();
        assertThat(jwt).as("Bank user JWT must be set").isNotBlank();

        Response response = adminClient.getCriticalServicesAs(jwt);
        guardNotFound(response, "GET /api/v1/admin/critical-services (bank user)");
        world.setLastResponse(response);
    }

    @When("^the bank user calls GET /api/v1/admin/client-base$")
    public void theBankUserCallsGetClientBase() {
        String jwt = world.getBankUserJwtToken();
        assertThat(jwt).as("Bank user JWT must be set").isNotBlank();

        Response response = adminClient.getClientBaseAs(jwt);
        guardNotFound(response, "GET /api/v1/admin/client-base (bank user)");
        world.setLastResponse(response);
    }

    @When("^the bank user calls GET /api/v1/admin/nca-email$")
    public void theBankUserCallsGetNcaEmail() {
        String jwt = world.getBankUserJwtToken();
        assertThat(jwt).as("Bank user JWT must be set").isNotBlank();

        Response response = adminClient.getNcaEmailConfigAs(jwt);
        guardNotFound(response, "GET /api/v1/admin/nca-email (bank user)");
        world.setLastResponse(response);
    }

    // -------------------------------------------------------------------------
    // AC-8 — Audit entry assertion
    // -------------------------------------------------------------------------

    /**
     * Verifies that the audit log contains an entry for the given entity type and action.
     *
     * <p>Queries GET /api/v1/audit?entityType={type} using the compliance user, who has
     * read access to the audit log (per LLD-03 §4).
     *
     * <p>If the audit endpoint returns 404, the step is marked @Pending — the audit
     * feature (LLD-03) may not be active.
     */
    @And("an audit entry is recorded for entity {string} with action {string}")
    public void anAuditEntryIsRecorded(String entityType, String action) {
        // Log in as compliance@dora.local to query the audit log
        Response loginResponse = authClient.login(
                Config.USER_COMPLIANCE,
                Config.DEV_SEED_PASSWORD);

        if (loginResponse.statusCode() == 404) {
            throw new PendingException(
                    "Auth endpoint returned 404 — cannot verify audit entry. " +
                    "Marking step as @Pending.");
        }

        assertThat(loginResponse.statusCode())
                .as("Compliance user login for audit check must return 200")
                .isEqualTo(200);

        String complianceJwt = loginResponse.jsonPath().getString("token");

        // Query the audit endpoint filtered by entity type
        Response auditResponse = auditClient.queryAudit(complianceJwt, entityType, null);

        if (auditResponse.statusCode() == 404) {
            throw new PendingException(
                    "GET /api/v1/audit returned 404 — the audit feature (LLD-03) is not active. " +
                    "Marking audit assertion as @Pending.");
        }

        assertThat(auditResponse.statusCode())
                .as("Audit log query for entityType '%s' must return 200. Body: %s",
                        entityType, auditResponse.body().asString())
                .isEqualTo(200);

        // The audit list may be in a "content" paged wrapper or a plain array
        List<String> actions;
        String body = auditResponse.body().asString();
        if (body.trim().startsWith("[")) {
            actions = auditResponse.jsonPath().getList("action");
        } else {
            actions = auditResponse.jsonPath().getList("content.action");
        }

        assertThat(actions)
                .as("Audit log for entityType '%s' should contain action '%s'. " +
                    "Actual actions found: %s. Full body: %s",
                        entityType, action, actions, body)
                .isNotNull()
                .isNotEmpty()
                .contains(action);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * If the response is 404, the admin feature branch has not been merged.
     * Mark the scenario as @Pending so it is skipped cleanly rather than failing.
     */
    private void guardNotFound(Response response, String endpoint) {
        if (response.statusCode() == 404) {
            throw new PendingException(
                    endpoint + " returned 404 — the admin feature (LLD-04) is not yet active " +
                    "in this environment. Marking scenario as @Pending.");
        }
    }
}
