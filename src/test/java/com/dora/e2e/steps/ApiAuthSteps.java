package com.dora.e2e.steps;

import com.dora.e2e.clients.AuthApiClient;
import com.dora.e2e.support.Config;
import com.dora.e2e.support.World;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for all API-level authentication scenarios (RestAssured, no browser).
 *
 * <p>These steps cover AC-1 through AC-5:
 * <ul>
 *   <li>AC-1 — valid login returns a JWT</li>
 *   <li>AC-2 — authenticated GET /me returns profile + role</li>
 *   <li>AC-3 — unauthenticated request → 401</li>
 *   <li>AC-4 — RBAC enforcement (PLATFORM_ADMIN blocked from incident probe)</li>
 *   <li>AC-5 — wrong password → 401 with generic message</li>
 * </ul>
 */
public class ApiAuthSteps {

    private final World world;
    private final AuthApiClient authClient;

    public ApiAuthSteps(World world) {
        this.world = world;
        this.authClient = new AuthApiClient();
    }

    // -------------------------------------------------------------------------
    // Given
    // -------------------------------------------------------------------------

    @Given("the API is running at {string}")
    public void theApiIsRunningAt(String url) {
        // The stack health gate in Hooks.@BeforeAll already verified reachability.
        // This step exists for Gherkin readability; no additional action is needed.
        assertThat(Config.API_BASE_URL)
                .as("API base URL must match the scenario expectation")
                .isEqualTo(url);
    }

    @Given("I have logged in as {string} with password {string}")
    public void iHaveLoggedInAs(String email, String password) {
        Response response = authClient.login(email, password);
        assertThat(response.statusCode())
                .as("Login pre-condition: expected HTTP 200 for user %s", email)
                .isEqualTo(200);

        String token = response.jsonPath().getString("token");
        assertThat(token)
                .as("Login pre-condition: JWT token must be present for user %s", email)
                .isNotBlank();

        world.setJwtToken(token);
        world.setLastResponse(response);
    }

    // -------------------------------------------------------------------------
    // When
    // -------------------------------------------------------------------------

    @When("^I POST /api/v1/auth/login with email \"([^\"]*)\" and password \"([^\"]*)\"$")
    public void iPostLoginWithEmailAndPassword(String email, String password) {
        Response response = authClient.login(email, password);
        world.setLastResponse(response);
    }

    @When("^I GET /api/v1/auth/me with the JWT$")
    public void iGetMeWithTheJwt() {
        String jwt = world.getJwtToken();
        assertThat(jwt)
                .as("JWT token must be available before calling /me")
                .isNotBlank();

        Response response = authClient.getMe(jwt);
        world.setLastResponse(response);
    }

    @When("^I GET /api/v1/auth/me without any Authorization header$")
    public void iGetMeWithoutAnyAuthorizationHeader() {
        Response response = authClient.getMeWithoutToken();
        world.setLastResponse(response);
    }

    @When("^I GET /api/v1/incidents/_probe with the JWT$")
    public void iGetIncidentProbeWithTheJwt() {
        String jwt = world.getJwtToken();
        assertThat(jwt)
                .as("JWT token must be available before calling /incidents/_probe")
                .isNotBlank();

        Response response = authClient.getIncidentProbe(jwt);
        world.setLastResponse(response);
    }

    // -------------------------------------------------------------------------
    // Then / And
    // -------------------------------------------------------------------------

    @Then("the response status is {int}")
    public void theResponseStatusIs(int expectedStatus) {
        assertThat(world.getLastResponse().statusCode())
                .as("Expected HTTP status %d but got %d. Response body: %s",
                        expectedStatus,
                        world.getLastResponse().statusCode(),
                        world.getLastResponse().body().asString())
                .isEqualTo(expectedStatus);
    }

    @And("the response body contains a non-empty {string} field")
    public void theResponseBodyContainsANonEmptyField(String fieldName) {
        String value = world.getLastResponse().jsonPath().getString(fieldName);
        assertThat(value)
                .as("Response field '%s' must be non-empty", fieldName)
                .isNotBlank();
    }

    @And("the response body contains {string}")
    public void theResponseBodyContainsField(String fieldName) {
        // Verify the field exists and is non-null
        Object value = world.getLastResponse().jsonPath().get(fieldName);
        assertThat(value)
                .as("Response body must contain a non-null '%s' field", fieldName)
                .isNotNull();
    }

    @And("the response body contains email {string}")
    public void theResponseBodyContainsEmail(String expectedEmail) {
        String actualEmail = world.getLastResponse().jsonPath().getString("email");
        assertThat(actualEmail)
                .as("Response 'email' field should be '%s'", expectedEmail)
                .isEqualToIgnoringCase(expectedEmail);
    }

    @And("the response body contains role {string}")
    public void theResponseBodyContainsRole(String expectedRole) {
        // The role may be in a "role" string field or a "roles" array — try both.
        String singleRole = world.getLastResponse().jsonPath().getString("role");
        if (singleRole != null) {
            assertThat(singleRole)
                    .as("Response 'role' field should be '%s'", expectedRole)
                    .isEqualToIgnoringCase(expectedRole);
            return;
        }

        // Try roles array
        java.util.List<String> roles = world.getLastResponse().jsonPath().getList("roles");
        assertThat(roles)
                .as("Response 'roles' array should contain '%s'", expectedRole)
                .isNotNull()
                .anySatisfy(r -> assertThat(r).isEqualToIgnoringCase(expectedRole));
    }

    @And("the response body {string} field does not contain {string}")
    public void theResponseBodyFieldDoesNotContain(String fieldName, String forbiddenText) {
        String value = world.getLastResponse().jsonPath().getString(fieldName);
        // If the field is absent entirely that is also acceptable — it means no sensitive data leaked.
        if (value != null) {
            assertThat(value.toLowerCase())
                    .as("Response field '%s' must not contain the text '%s' (information leakage)",
                            fieldName, forbiddenText)
                    .doesNotContain(forbiddenText.toLowerCase());
        }
    }
}
