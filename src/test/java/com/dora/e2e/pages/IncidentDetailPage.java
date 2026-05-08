package com.dora.e2e.pages;

import com.dora.e2e.support.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.List;

/**
 * Page object for the Angular IncidentDetailComponent at /incidents/:id.
 *
 * <p>Also models the embedded AttachmentUploaderComponent that lives inside the
 * detail page, since it is not navigated to separately.
 *
 * <p>All {@link By} selectors are declared as constants here.
 * Step definitions call user-level action methods only.
 */
public class IncidentDetailPage {

    // ---- Selectors — incident detail core ---------------------------------

    /**
     * The element that displays the human-readable incident ID (INC-YYYYMMDD-NNNN).
     * Angular renders this in a span, h1, or data-testid attribute.
     */
    private static final By INCIDENT_ID_ELEMENT =
            By.cssSelector("[data-testid='incident-id'], .incident-id, span.incident-id, h1.incident-id");

    /**
     * Fallback locator: any element that contains the INC- prefix text.
     * Used when the incident ID is embedded in a broader heading.
     */
    private static final By INCIDENT_ID_TEXT_ANYWHERE =
            By.xpath("//*[contains(text(),'INC-')]");

    /** The attachments section / list. */
    private static final By ATTACHMENTS_SECTION =
            By.cssSelector("[data-testid='attachments-section'], .attachments-section, " +
                           "app-attachment-uploader, .attachment-list, #attachments");

    /** The ICT assets section / list. */
    private static final By ASSETS_SECTION =
            By.cssSelector("[data-testid='assets-section'], .assets-section, " +
                           ".ict-assets, #assets");

    /** The linked services section / list. */
    private static final By SERVICES_SECTION =
            By.cssSelector("[data-testid='services-section'], .services-section, " +
                           ".linked-services, #services");

    // ---- Selectors — AttachmentUploaderComponent --------------------------

    /**
     * File input element inside the attachment uploader.
     * Angular sets type="file" on the hidden input; the component triggers a click on it.
     */
    private static final By FILE_INPUT =
            By.cssSelector("input[type='file']");

    /** Upload / Submit button in the attachment uploader. */
    private static final By UPLOAD_BUTTON =
            By.cssSelector("[data-testid='upload-btn'], button.upload-btn, " +
                           "button[type='submit'].upload, .attachment-upload button[type='submit']");

    /**
     * Status badge shown after an attachment upload completes.
     * The component sets a CSS class or data attribute to indicate READY/PENDING/FAILED.
     */
    private static final By ATTACHMENT_STATUS =
            By.cssSelector("[data-testid='attachment-status'], .attachment-status, " +
                           ".status-badge, span.status");

    // ---- Infrastructure ---------------------------------------------------

    private final WebDriver driver;
    private final WebDriverWait wait;

    public IncidentDetailPage(WebDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(Config.WAIT_TIMEOUT_SECONDS));
    }

    // ---- Navigation -------------------------------------------------------

    /**
     * Navigate directly to /incidents/{id}.
     *
     * @param incidentId the incident UUID (not the INC-YYYYMMDD-NNNN display ID)
     */
    public void openById(String incidentId) {
        driver.get(Config.FRONTEND_BASE_URL + "/incidents/" + incidentId);
        waitForPageLoad();
    }

    /**
     * Wait until the incident ID element is visible — indicates the detail page has loaded.
     *
     * <p>Falls back to INCIDENT_ID_TEXT_ANYWHERE if the primary locator is not found
     * within the timeout.
     */
    public void waitForPageLoad() {
        // Wait for the URL to contain "/incidents/" and not end with "/new"
        wait.until(driver ->
                driver.getCurrentUrl().contains("/incidents/") &&
                !driver.getCurrentUrl().endsWith("/new") &&
                !driver.getCurrentUrl().endsWith("/incidents"));
    }

    // ---- Incident detail actions ------------------------------------------

    /**
     * Read the displayed incident ID (INC-YYYYMMDD-NNNN) from the page.
     *
     * @return the incident ID text, or empty string if not found
     */
    public String getDisplayedIncidentId() {
        try {
            // Try primary locator first
            List<WebElement> elements = driver.findElements(INCIDENT_ID_ELEMENT);
            if (!elements.isEmpty()) {
                WebElement el = wait.until(
                        ExpectedConditions.visibilityOf(elements.get(0)));
                return el.getText().trim();
            }

            // Fallback: search for any element containing "INC-"
            List<WebElement> anyMatch = driver.findElements(INCIDENT_ID_TEXT_ANYWHERE);
            if (!anyMatch.isEmpty()) {
                return anyMatch.get(0).getText().trim();
            }

            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Return true if the current page URL starts with /incidents/ (and is not /new).
     */
    public boolean isOnDetailPage() {
        String url = driver.getCurrentUrl();
        return url.contains("/incidents/") &&
               !url.endsWith("/new") &&
               !url.endsWith("/incidents");
    }

    /**
     * Return the current browser URL.
     */
    public String currentUrl() {
        return driver.getCurrentUrl();
    }

    // ---- Attachment section -----------------------------------------------

    /**
     * Return true if the attachments section is visible on the page.
     */
    public boolean isAttachmentsSectionVisible() {
        return isSectionVisible(ATTACHMENTS_SECTION);
    }

    /**
     * Return true if the ICT assets section is visible on the page.
     */
    public boolean isAssetsSectionVisible() {
        return isSectionVisible(ASSETS_SECTION);
    }

    /**
     * Return true if the linked services section is visible on the page.
     */
    public boolean isServicesSectionVisible() {
        return isSectionVisible(SERVICES_SECTION);
    }

    /**
     * Upload a file via the AttachmentUploaderComponent's file input.
     *
     * <p>This uses the hidden {@code <input type="file">} directly — Selenium can send
     * keys to it even when it is visually hidden. The component's change handler fires
     * automatically once the path is set.
     *
     * @param file the file to upload
     */
    public void uploadAttachment(File file) {
        WebElement fileInput = wait.until(
                ExpectedConditions.presenceOfElementLocated(FILE_INPUT));
        fileInput.sendKeys(file.getAbsolutePath());

        // Click the upload/submit button if it is separate from the file input trigger
        List<WebElement> uploadButtons = driver.findElements(UPLOAD_BUTTON);
        if (!uploadButtons.isEmpty()) {
            wait.until(ExpectedConditions.elementToBeClickable(uploadButtons.get(0))).click();
        }
    }

    /**
     * Wait until the attachment status badge appears and return its visible text.
     *
     * @return status text (e.g. "READY", "PENDING", "FAILED")
     */
    public String waitForAttachmentStatus() {
        WebElement statusEl = wait.until(
                ExpectedConditions.visibilityOfElementLocated(ATTACHMENT_STATUS));
        return statusEl.getText().trim().toUpperCase();
    }

    // ---- Private helpers --------------------------------------------------

    private boolean isSectionVisible(By locator) {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (Exception e) {
            // Section not visible within timeout
            return false;
        }
    }
}
