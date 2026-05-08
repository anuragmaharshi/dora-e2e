package com.dora.e2e.steps;

import com.dora.e2e.clients.IncidentApiClient;
import com.dora.e2e.clients.AuthApiClient;
import com.dora.e2e.pages.IncidentCreatePage;
import com.dora.e2e.pages.IncidentDetailPage;
import com.dora.e2e.pages.LoginPage;
import com.dora.e2e.support.Config;
import com.dora.e2e.support.World;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for LLD-05: Incident Logging — UI (Selenium) scenarios.
 *
 * <p>All browser interactions go through {@link IncidentCreatePage} and
 * {@link IncidentDetailPage}. No raw {@link org.openqa.selenium.By} locators
 * appear in this class.
 *
 * <p>API calls used for test-data setup go through {@link IncidentApiClient}.
 * Authentication uses {@link AuthApiClient}.
 *
 * <p>Injected with {@link World} via PicoContainer — one World per scenario.
 */
public class IncidentUiSteps {

    private final World world;
    private final IncidentApiClient incidentClient;
    private final AuthApiClient authClient;

    public IncidentUiSteps(World world) {
        this.world          = world;
        this.incidentClient = new IncidentApiClient();
        this.authClient     = new AuthApiClient();
    }

    // -------------------------------------------------------------------------
    // Background — UI login
    // -------------------------------------------------------------------------

    /**
     * Logs ops@dora.local in via the browser.
     * The @Before(value="@ui") hook in Hooks.java already started a WebDriver
     * before this step runs.
     */
    @Given("the ops analyst is logged in via the browser")
    public void theOpsAnalystIsLoggedInViaBrowser() {
        WebDriver driver = world.getDriver();
        assertThat(driver)
                .as("WebDriver must be initialised — scenario must be tagged @ui")
                .isNotNull();

        LoginPage loginPage = new LoginPage(driver);
        loginPage.open();
        loginPage.login(Config.USER_OPS_ANALYST, Config.DEV_SEED_PASSWORD);

        // Wait until the URL leaves /login — indicates successful authentication
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(Config.WAIT_TIMEOUT_SECONDS));
        wait.until(ExpectedConditions.not(
                ExpectedConditions.urlContains("/login")));

        // Also obtain and cache the JWT for API-level setup steps that run in the same scenario
        Response loginResponse = authClient.login(Config.USER_OPS_ANALYST, Config.DEV_SEED_PASSWORD);
        if (loginResponse.statusCode() == 200) {
            String token = loginResponse.jsonPath().getString("token");
            if (token != null && !token.isBlank()) {
                world.setJwtToken(token);
            }
        }
    }

    // -------------------------------------------------------------------------
    // AC-1 UI — Navigate to /incidents/new and create an incident
    // -------------------------------------------------------------------------

    @When("the ops analyst navigates to the incident creation page")
    public void theOpsAnalystNavigatesToIncidentCreationPage() {
        WebDriver driver = world.getDriver();
        assertThat(driver).as("WebDriver must be initialised").isNotNull();

        IncidentCreatePage createPage = new IncidentCreatePage(driver);
        createPage.open();
    }

    @And("fills in the incident title {string}")
    public void fillsInTheIncidentTitle(String title) {
        WebDriver driver = world.getDriver();
        assertThat(driver).as("WebDriver must be initialised").isNotNull();

        IncidentCreatePage createPage = new IncidentCreatePage(driver);
        createPage.enterTitle(title);
    }

    @And("selects severity {string}")
    public void selectsSeverity(String severity) {
        WebDriver driver = world.getDriver();
        assertThat(driver).as("WebDriver must be initialised").isNotNull();

        IncidentCreatePage createPage = new IncidentCreatePage(driver);
        createPage.selectSeverity(severity);
    }

    @And("submits the incident form")
    public void submitsTheIncidentForm() {
        WebDriver driver = world.getDriver();
        assertThat(driver).as("WebDriver must be initialised").isNotNull();

        IncidentCreatePage createPage = new IncidentCreatePage(driver);
        createPage.submit();
    }

    @Then("the browser navigates to the incident detail page")
    public void theBrowserNavigatesToIncidentDetailPage() {
        WebDriver driver = world.getDriver();
        assertThat(driver).as("WebDriver must be initialised").isNotNull();

        IncidentDetailPage detailPage = new IncidentDetailPage(driver);
        detailPage.waitForPageLoad();

        assertThat(detailPage.isOnDetailPage())
                .as("Browser should be on an incident detail page (/incidents/:id) " +
                    "after form submission. Actual URL: %s", detailPage.currentUrl())
                .isTrue();
    }

    @Then("the incident detail page shows an incident ID matching {string}")
    public void theIncidentDetailPageShowsIncidentIdMatching(String prefix) {
        WebDriver driver = world.getDriver();
        assertThat(driver).as("WebDriver must be initialised").isNotNull();

        IncidentDetailPage detailPage = new IncidentDetailPage(driver);
        String displayedId = detailPage.getDisplayedIncidentId();

        assertThat(displayedId)
                .as("Incident detail page must display an ID starting with '%s'. " +
                    "Displayed text: '%s'. URL: %s", prefix, displayedId, detailPage.currentUrl())
                .contains(prefix);
    }

    // -------------------------------------------------------------------------
    // AC-3 UI — Upload attachment via AttachmentUploaderComponent
    // -------------------------------------------------------------------------

    /**
     * Creates an incident via the API (not via UI) and stores its UUID in World.
     * This setup step lets the attachment upload scenario start at the detail page
     * directly, without repeating the create-incident UI flow.
     */
    @Given("an incident has been created via the API by the ops analyst with title {string}")
    public void anIncidentCreatedViaApiByOpsAnalyst(String title) {
        String jwt = world.getJwtToken();
        assertThat(jwt)
                .as("OPS_ANALYST JWT must be cached (Background step must have run first)")
                .isNotBlank();

        String uniqueTitle = title + " " + UUID.randomUUID().toString().substring(0, 8);
        Response createResponse = incidentClient.createIncident(jwt, uniqueTitle, "MEDIUM");

        assertThat(createResponse.statusCode())
                .as("API incident creation must return 201. Body: %s",
                        createResponse.body().asString())
                .isEqualTo(201);

        String id = createResponse.jsonPath().getString("id");
        assertThat(id).as("Created incident must have a non-blank id").isNotBlank();
        world.setLastIncidentId(id);
    }

    @When("the ops analyst navigates to the incident detail page for that incident")
    public void theOpsAnalystNavigatesToIncidentDetailPage() {
        WebDriver driver    = world.getDriver();
        String incidentId   = world.getLastIncidentId();
        assertThat(driver).as("WebDriver must be initialised").isNotNull();
        assertThat(incidentId).as("Incident ID must be set in World").isNotBlank();

        IncidentDetailPage detailPage = new IncidentDetailPage(driver);
        detailPage.openById(incidentId);
    }

    @And("uploads a file named {string} via the attachment uploader")
    public void uploadsAFileNamedViaAttachmentUploader(String filename) {
        WebDriver driver = world.getDriver();
        assertThat(driver).as("WebDriver must be initialised").isNotNull();

        // Create a temp file with the given name and a small text payload
        File tempFile = createTempFile(filename, "E2E test evidence content");

        IncidentDetailPage detailPage = new IncidentDetailPage(driver);
        detailPage.uploadAttachment(tempFile);

        tempFile.deleteOnExit();
    }

    @Then("the attachment appears in the incident detail with status {string}")
    public void theAttachmentAppearsWithStatus(String expectedStatus) {
        WebDriver driver = world.getDriver();
        assertThat(driver).as("WebDriver must be initialised").isNotNull();

        IncidentDetailPage detailPage = new IncidentDetailPage(driver);
        String actualStatus = detailPage.waitForAttachmentStatus();

        assertThat(actualStatus)
                .as("Attachment status must be '%s' after upload. Actual: '%s'. URL: %s",
                        expectedStatus, actualStatus, detailPage.currentUrl())
                .isEqualToIgnoringCase(expectedStatus);
    }

    // -------------------------------------------------------------------------
    // AC-6 UI — Full incident detail sections visible
    // -------------------------------------------------------------------------

    /**
     * Creates an incident with an attachment and an asset via the API so the
     * UI detail-page assertions can verify all three sections are displayed.
     */
    @Given("an incident with an attachment and an asset exists via the API")
    public void anIncidentWithAttachmentAndAssetExistsViaApi() {
        String jwt = world.getJwtToken();
        assertThat(jwt)
                .as("OPS_ANALYST JWT must be cached (Background step must have run first)")
                .isNotBlank();

        // Create incident
        String title = "UI Full Detail " + UUID.randomUUID().toString().substring(0, 8);
        Response createResponse = incidentClient.createIncident(jwt, title, "HIGH");

        assertThat(createResponse.statusCode())
                .as("API incident creation must return 201. Body: %s",
                        createResponse.body().asString())
                .isEqualTo(201);

        String incidentId = createResponse.jsonPath().getString("id");
        assertThat(incidentId).as("Created incident must have an id").isNotBlank();
        world.setLastIncidentId(incidentId);

        // Request and complete an attachment upload
        Response attachResponse = incidentClient.requestAttachmentUpload(
                jwt, incidentId, "evidence.pdf", "application/pdf");
        if (attachResponse.statusCode() == 201) {
            String attachmentId = attachResponse.jsonPath().getString("attachmentId");
            String presignedUrl = attachResponse.jsonPath().getString("uploadUrl");
            if (presignedUrl != null && !presignedUrl.isBlank()) {
                incidentClient.uploadToPresignedUrl(
                        presignedUrl, "%PDF-1.4 ui-seed".getBytes(), "application/pdf");
                if (attachmentId != null) {
                    incidentClient.completeAttachmentUpload(jwt, incidentId, attachmentId);
                }
            }
        }

        // Link an asset
        incidentClient.linkAsset(jwt, incidentId, "UI Test Router", "HARDWARE");
    }

    @Then("the incident detail page shows the attachments section")
    public void theIncidentDetailPageShowsAttachmentsSection() {
        WebDriver driver = world.getDriver();
        assertThat(driver).as("WebDriver must be initialised").isNotNull();

        IncidentDetailPage detailPage = new IncidentDetailPage(driver);
        assertThat(detailPage.isAttachmentsSectionVisible())
                .as("The attachments section must be visible on the incident detail page. " +
                    "URL: %s", detailPage.currentUrl())
                .isTrue();
    }

    @Then("the incident detail page shows the assets section")
    public void theIncidentDetailPageShowsAssetsSection() {
        WebDriver driver = world.getDriver();
        assertThat(driver).as("WebDriver must be initialised").isNotNull();

        IncidentDetailPage detailPage = new IncidentDetailPage(driver);
        assertThat(detailPage.isAssetsSectionVisible())
                .as("The ICT assets section must be visible on the incident detail page. " +
                    "URL: %s", detailPage.currentUrl())
                .isTrue();
    }

    @Then("the incident detail page shows the linked services section")
    public void theIncidentDetailPageShowsLinkedServicesSection() {
        WebDriver driver = world.getDriver();
        assertThat(driver).as("WebDriver must be initialised").isNotNull();

        IncidentDetailPage detailPage = new IncidentDetailPage(driver);
        assertThat(detailPage.isServicesSectionVisible())
                .as("The linked services section must be visible on the incident detail page. " +
                    "URL: %s", detailPage.currentUrl())
                .isTrue();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Create a temporary file with the given name and content.
     * The file is created in the system temp directory.
     */
    private File createTempFile(String filename, String content) {
        try {
            File tempDir = new File(System.getProperty("java.io.tmpdir"));
            File file = new File(tempDir, filename);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
            }
            return file;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create temp file '" + filename + "': " + e.getMessage(), e);
        }
    }
}
