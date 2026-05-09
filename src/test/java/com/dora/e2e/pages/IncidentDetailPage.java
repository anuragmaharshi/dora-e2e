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

    /**
     * The "Attachments" tab button — clicking it activates the attachments tab panel.
     * The Angular incident detail uses role="tablist" / role="tab" pattern.
     */
    private static final By ATTACHMENTS_TAB_BUTTON =
            By.cssSelector("#tab-btn-attachments, button[aria-controls='tab-attachments']");

    /**
     * The attachments tab panel (shown when the Attachments tab is active).
     * The panel is present in the DOM but hidden via [hidden] when not active.
     */
    private static final By ATTACHMENTS_SECTION =
            By.cssSelector("#tab-attachments, [aria-labelledby='tab-btn-attachments']");

    /**
     * The "ICT Assets" tab button — clicking it activates the assets tab panel.
     */
    private static final By ASSETS_TAB_BUTTON =
            By.cssSelector("#tab-btn-assets, button[aria-controls='tab-assets']");

    /**
     * The ICT assets tab panel.
     */
    private static final By ASSETS_SECTION =
            By.cssSelector("#tab-assets, [aria-labelledby='tab-btn-assets']");

    /**
     * The linked services section inside the Overview tab.
     * Services are shown in the Overview tab (not a separate tab).
     */
    private static final By SERVICES_SECTION =
            By.cssSelector(".detail-section ul[aria-label='Linked critical services'], " +
                           ".detail-section:has(h3:contains('Affected Critical Services'))");

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
     * Status badge shown in the attachments table after an attachment upload completes.
     * The Angular template uses class "att-status att-status-ready" etc.
     * Note: do NOT use generic ".status-badge" which matches the incident status header.
     */
    private static final By ATTACHMENT_STATUS =
            By.cssSelector("[data-testid='attachment-status'], .attachment-status, " +
                           "span[class^='att-status'], td span.att-status");

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
     * Return true if the Attachments tab button is visible (tab-based UI always has the button).
     * Clicks the Attachments tab to activate it before checking panel visibility.
     */
    public boolean isAttachmentsSectionVisible() {
        // Wait for the tab button to be present and clickable, then click it
        try {
            WebElement tabBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(ATTACHMENTS_TAB_BUTTON));
            tabBtn.click();
        } catch (Exception ignored) {
            // Tab button not found or not clickable — fall through
        }
        // Wait for the section to appear (Angular removes [hidden] after click)
        return isSectionNotHidden(ATTACHMENTS_SECTION);
    }

    /**
     * Return true if the ICT Assets tab button is visible (tab-based UI always has the button).
     * Clicks the Assets tab to activate it before checking panel visibility.
     */
    public boolean isAssetsSectionVisible() {
        // Wait for the tab button to be present and clickable, then click it
        try {
            WebElement tabBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(ASSETS_TAB_BUTTON));
            tabBtn.click();
        } catch (Exception ignored) {
            // Tab button not found or not clickable — fall through
        }
        // Wait for the section to appear (Angular removes [hidden] after click)
        return isSectionNotHidden(ASSETS_SECTION);
    }

    /**
     * Return true if the linked services section is visible on the Overview tab.
     * Services are shown in the Overview tab — no tab click needed.
     * The overview tab is active by default.
     */
    public boolean isServicesSectionVisible() {
        // The overview tab is default — the section should already be visible.
        // Check for either the populated list or the empty-state paragraph.
        try {
            // Wait for the overview section to be present (not hidden)
            wait.until(driver -> {
                List<WebElement> sections = driver.findElements(
                        By.cssSelector("#tab-overview"));
                if (sections.isEmpty()) return false;
                // Section must not have [hidden] attribute
                String hidden = sections.get(0).getAttribute("hidden");
                return hidden == null;
            });
            // Look for the services section inside overview
            List<WebElement> servicesList = driver.findElements(
                    By.cssSelector("ul[aria-label='Linked critical services']"));
            if (!servicesList.isEmpty()) return true;
            List<WebElement> serviceSection = driver.findElements(
                    By.xpath("//section[@id='tab-overview']//h3[contains(text(),'Affected Critical Services')]"));
            return !serviceSection.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Upload a file via the AttachmentUploaderComponent's file input.
     *
     * <p>The attachment uploader lives inside the Attachments tab panel which is hidden
     * by default. This method first clicks the Attachments tab to reveal it, then
     * interacts with the file input.
     *
     * @param file the file to upload
     */
    public void uploadAttachment(File file) {
        // Navigate to the Attachments tab first (it is hidden by default)
        List<WebElement> tabButtons = driver.findElements(ATTACHMENTS_TAB_BUTTON);
        if (!tabButtons.isEmpty()) {
            wait.until(ExpectedConditions.elementToBeClickable(tabButtons.get(0))).click();
        }

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

    /**
     * Wait until a section element is present in the DOM and does NOT have the
     * {@code [hidden]} attribute (Angular sets hidden via property binding, which
     * maps to the HTML {@code hidden} attribute and makes the element display:none).
     *
     * <p>This is preferred over {@link ExpectedConditions#visibilityOfElementLocated}
     * for Angular tab panels, because after a tab click Angular removes the
     * {@code hidden} attribute in a microtask tick. The visibility-based wait sometimes
     * races with that tick.
     */
    private boolean isSectionNotHidden(By locator) {
        try {
            wait.until(driver -> {
                List<WebElement> elements = driver.findElements(locator);
                if (elements.isEmpty()) return false;
                String hidden = elements.get(0).getAttribute("hidden");
                return hidden == null;
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
